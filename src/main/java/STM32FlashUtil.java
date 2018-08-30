import gnu.getopt.Getopt;
import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;

import org.stm32flash.*;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static java.lang.System.exit;

public class STM32FlashUtil {
    public static void main(String[] args) {
        String port = "/dev/ttyUSB0";
        int baudRate = SerialPort.BAUDRATE_256000;
        String dumpFilename = "/tmp/fw.bin";
        String flashFilename = "/tmp/fw.bin";
        boolean doReset = false;
        boolean doErase = false;
        String partialEraseRange = null;
        boolean doFlash = false;
        boolean doVerify = false;
        boolean doDump = false;

        int verbose = 0;

        final Getopt getopt = new Getopt("stm32flash-util", args, "eE:rvf:p:d:b:V");

        int arg = -1;
        while ((arg = getopt.getopt()) != -1) {
            switch (arg) {
                case 'b':
                    baudRate = Integer.parseInt(getopt.getOptarg());
                    break;
                case 'E':
                    partialEraseRange = getopt.getOptarg();
                    // fallthrough
                case 'e':
                    doErase = true;
                    break;
                case 'r':
                    doReset = true;
                    break;
                case 'f':
                    doFlash = true;
                    flashFilename = getopt.getOptarg();
                    break;
                case 'v':
                    doVerify = true;
                    break;
                case 'd':
                    doDump = true;
                    dumpFilename = getopt.getOptarg();
                    break;
                case 'p':
                    port = getopt.getOptarg();
                    break;
                case 'V':
                    verbose++;
                    break;
            }
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
            System.out.println("Connected to " + d.getName() + " / 0x" + Integer.toHexString(d.getId()));
            if (verbose > 0)
                System.out.println("DeviceInfo: " + d);

            if (doDump) {
                System.out.println("Dumping target firmware.");
                byte[] buffer = flasher.dumpFirmware();
                if (dumpFilename != null) {
                    System.out.println("Saving firmware to " + dumpFilename);
                    FileUtils.writeByteArrayToFile(new File(dumpFilename), buffer);
                } else {
                    System.err.println("Could not dump target firmware.");
                    exit(-1);
                }
            }

            if (doErase) {
                if (partialEraseRange != null) {
                    String[] params = partialEraseRange.split(":");

                    if (params.length != 2) {
                        System.err.println("bad partial erase parameter, expecting \"startaddress:lenght\" : " + partialEraseRange);
                        exit(-1);
                    }

                    int startAddress = Integer.decode(params[0]);
                    int len = Integer.decode(params[1]);

                    if (startAddress == 0)
                        startAddress = d.getFlashStart();

                    if (startAddress < d.getFlashStart()) {
                        System.err.println("bad partial erase parameter, start address is below flash start.");
                        exit(-1);
                    }

                    if (startAddress + len > d.getFlashStart() + d.getFlashSize()) {
                        System.err.println("bad partial erase parameter, end address is after flash end.");
                        exit(-1);
                    }

                    System.out.println("Erasing target from 0x" + Integer.toHexString(startAddress) + " to 0x" + Integer.toHexString(startAddress+len) );
                    if (!flasher.erase(startAddress, len))
                        exit(-1);
                } else {
                    System.out.println("Erasing target firmware.");
                    if (!flasher.eraseFirmware())
                        exit(-1);
                }
            }

            if (doFlash) {
                System.out.println("Flashing " + flashFilename + " to target.");

                STM32Firmware fw = null;
                try {
                    fw = new STM32Firmware(flashFilename);
                } catch (Exception e) {
                    e.printStackTrace();
                    exit(-1);
                }

                System.out.println("Loaded " + fw + ", md5=" + Hex.encodeHexString( fw.getChecksum() ));
                if (!flasher.flashFirmware(fw.getBuffer(), STM32Flasher.EraseMode.Partial, doVerify)) {
                    System.err.println("Firmware flashing failed.");
                    exit(-1);
                }

                System.out.println("Firmware was flashed successfully.");
            }

            if (doReset) {
                if (flasher.resetDevice())
                    exit(-1);
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
                    System.out.println("read bytes " + Hex.encodeHexString( buffer ));

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
                System.out.println("write bytes " + Hex.encodeHexString( bytes));
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


