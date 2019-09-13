import gnu.getopt.Getopt;
import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;

import org.stm32flash.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

import static java.lang.System.exit;
import static java.lang.System.in;

public class STM32FlashUtil {

    private static void help() {

        System.out.println("./stm32flash-util\n" +
                "\t-f path/to/file.bin     flash file to target flash memory\n" +
                "\t-p /dev/ttyUSB42        specify tty to use\n" +
                "\t-b 115200               specify baudrate to use\n" +
                "\t-v                      verify flash content while flashing\n" +
                "\t-d path/to/file.bin     dump target flash memory to file\n" +
                "\t-e                      erase target flash memory\n" +
                "\t-E 0x8000000:0x200      erase 0x200 bytes of flash memory from 0x8000000\n" +
                "\t-r                      lock target flash read\n" +
                "\t-R                      unlock target flash read (will erase all flash)\n" +
                "\t-l                      lock target flash write\n" +
                "\t-L                      unlock target flash write\n" +
                "\t-S 32k                  specify device flash size\n" +
                "\t-V                      verbose\n" +
                "\t-VV                     more verbose\n" +
                "\t-h                      print help\n"
        );
    }


    public static void main(String[] args) {
        String port = "/dev/ttyUSB0";
        int baudRate = SerialPort.BAUDRATE_256000;
        String dumpFilename = "/tmp/fw.bin";
        String flashFilename = "/tmp/fw.bin";
        boolean doReset = false;
        boolean doErase = false;
        String partialEraseRange = null;
        String flashSize = null;
        boolean doFlash = false;
        boolean doVerify = false;
        boolean doDump = false;
        boolean doLockFlashWrite = false;
        boolean doLockFlashRead = false;
        boolean doUnlockFlashWrite = false;
        boolean doUnlockFlashRead = false;

        ArrayList<Task> tasks = new ArrayList<Task>();

        int verbose = 0;

        final Getopt getopt = new Getopt("stm32flash-util", args, "heE:f:F:p:d:LlRrub:VS:");

        int arg = -1;
        try {
            while ((arg = getopt.getopt()) != -1) {
                switch (arg) {
                    case 'b':
                        baudRate = Integer.parseInt(getopt.getOptarg());
                        break;
                    case 'p':
                        port = getopt.getOptarg();
                        break;
                    case 'S':
                        flashSize = getopt.getOptarg();
                        break;
                    case 'V':
                        verbose++;
                        break;
                    case 'E':
                        tasks.add(new PartialEraseTask(getopt.getOptarg()));
                        break;
                    case 'e':
                        tasks.add(new FullEraseTask());
                        break;
                    case 'f':
                        tasks.add(new FlashFileTask(getopt.getOptarg(), false));
                        break;
                    case 'F':
                        tasks.add(new FlashFileTask(getopt.getOptarg(), true));

                        break;
                    case 'd':
                        tasks.add(new DumpFlashTask(getopt.getOptarg()));
                        break;
                    case 'l':
                        tasks.add(new DoLockFlashTask());
                        break;
                    case 'L':
                        tasks.add(new DoUnlockFlashTask());
                        break;
                    case 'r':
                        tasks.add(new DoLockFlashReadoutTask());
                        break;
                    case 'R':
                        tasks.add(new DoUnlockFlashReadoutTask());
                        break;
                    case 'h':
                        help();
                        exit(0);
                        break;
                    default:
                        help();
                        exit(1);
                }
            }
        } catch (Exception e) {
            help();
            e.printStackTrace();
        }

        System.out.println("stm32flash-util " + port + " at " + baudRate + "bps");

        SerialPort sp = new SerialPort(port);

        try {
            sp.openPort();
            sp.setParams(baudRate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_EVEN);
        } catch (SerialPortException e) {
            e.printStackTrace();
            exit(-2);
        }

        JsscSerialIface jsscIface = new JsscSerialIface(sp, verbose > 1);

        STM32Flasher flasher = new STM32Flasher(jsscIface, verbose > 0);

        flasher.registerProgressListener(new STM32OperationProgressListener() {
            @Override
            public void completed(boolean successfull) {
                if (!successfull)
                    System.err.println("failure");
                else
                    System.err.println("done");
            }

            @Override
            public void progress(long current, long total) {
                System.out.println("progress: " + current + " of " + total);
            }
        });
        try {
            if (!flasher.connect()) {
                System.err.println("Could not connect to target.");
                exit(-2);
            }

            STM32Device d = flasher.getDevice();

            if (flashSize != null) {
                int size = 0;
                if (flashSize.toLowerCase().endsWith("k"))
                    size = Integer.decode(flashSize.substring(0, flashSize.length() - 1)) * 1024;
                else if (flashSize.toLowerCase().endsWith("kb"))
                    size = Integer.decode(flashSize.substring(0, flashSize.length() - 2)) * 1024;
                else if (flashSize.toLowerCase().endsWith("m"))
                    size = Integer.decode(flashSize.substring(0, flashSize.length() - 1)) * 1024 * 1024;
                else if (flashSize.toLowerCase().endsWith("mb"))
                    size = Integer.decode(flashSize.substring(0, flashSize.length() - 2)) * 1024 * 1024;
                else
                    size = Integer.decode(flashSize);

                System.out.println("Setting flash size to : " + size + "b");
                d.setFlashSize(size);
            }

            System.out.println("Connected to " + d.getName() + " / 0x" + Integer.toHexString(d.getId()));
            if (verbose > 0)
                System.out.println("DeviceInfo: " + d);

            for (Task t : tasks) {
                if (!t.work(flasher)) {
                    System.err.println("Failed, Abort.");
                    exit(-1);
                }
            }

        } catch (TimeoutException e) {
            System.err.println(e);
            exit(-1);
        } catch (IOException e) {
            e.printStackTrace();
            exit(-1);
        }

        try {
            sp.closePort();
        } catch (SerialPortException e) {
            e.printStackTrace();
        }

        exit(0);
    }

    private static class JsscSerialIface extends STM32UsartInterface {
        private final SerialPort mSerialPort;
        boolean mDebug = false;

        public JsscSerialIface(SerialPort sp, boolean debug) {
            mSerialPort = sp;
            mDebug = debug;
        }

        @Override
        public byte[] read(int count, int timeout) throws IOException, TimeoutException {
            try {
                byte[] buffer = mSerialPort.readBytes(count, timeout);

                if (mDebug)
                    System.out.println("read bytes 0x" + Hex.encodeHexString( buffer ));

                return buffer;
            } catch (SerialPortException e) {
                throw new IOException(e);
            } catch (SerialPortTimeoutException e) {
                throw new TimeoutException("Timeout " + e.getMethodName() + " on " + e.getPortName() + " (" + + e.getTimeoutValue() + "ms)" );
            }
        }

        @Override
        public void write(byte[] bytes) throws IOException {
            if (mDebug)
                System.out.println("write bytes 0x" + Hex.encodeHexString( bytes));
            try {
                mSerialPort.writeBytes(bytes);
            } catch (SerialPortException e) {
                throw new IOException(e);
            }
        }

        public void setDebug(boolean d) {
            this.mDebug = d;
        }
    }
}


