import gnu.getopt.Getopt;
import jssc.SerialPort;
import jssc.SerialPortException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class STM32FlashUtil {
    public static void main(String[] args) {
        String port = "/dev/ttyUSB0";
        String dumpFilename = "/tmp/fw.bin";
        String flashFilename = "/tmp/fw.bin";
        boolean doReset = false;
        boolean doErase = false;
        boolean doFlash = false;
        boolean doVerify = false;
        boolean doDump = false;

        final Getopt getopt = new Getopt("STM32FlashUtil", args, "ervf:p:d:");

        int arg = -1;
        while ((arg = getopt.getopt()) != -1) {
            switch (arg) {
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
            }
        }

        System.out.println("STM32FlashUtil " + port);

        SerialPort sp = new SerialPort(port);

        try {
            sp.openPort();
            sp.setParams(SerialPort.BAUDRATE_256000, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_EVEN);
        } catch (SerialPortException e) {
            e.printStackTrace();
            return;
        }

        JsscSerialIface jsscIface = new JsscSerialIface(sp);

        STM32Flasher flasher = new STM32Flasher(jsscIface);

        try {
            if (!flasher.connect()) {
                System.err.println("Could not connect to target.");
                return;
            }

            STM32Device d = flasher.getDevice();
            System.out.println("Connected to " + d);

            if (doDump) {
                System.out.println("Dumping target firmware.");
                byte[] buffer = flasher.dumpFirmware();
                if (dumpFilename != null) {
                    System.out.println("Saving firmware to " + dumpFilename);
                    FileUtils.writeByteArrayToFile(new File(dumpFilename), buffer);
                }
            }

            if (doErase) {
                System.out.println("Erasing target firmware.");
                flasher.eraseFirmware();
            }

            if (doFlash) {
                System.out.println("Flashing " + flashFilename + " to target.");

                STM32Firmware fw;
                try {
                    fw = new STM32Firmware(flashFilename);
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }

                System.out.println("Loaded " + fw + ", md5=" + Hex.encodeHexString( fw.getChecksum() ));

                flasher.flashFirmware(fw.getBuffer(), doVerify);
            }

            if (doReset)
                flasher.resetDevice();

        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            sp.closePort();
        } catch (SerialPortException e) {
            e.printStackTrace();
        }

    }

    private static class JsscSerialIface extends STM32UsartInterface {
        private final SerialPort mSerialPort;
        boolean mDebug = false;

        public JsscSerialIface(SerialPort sp) {
            mSerialPort = sp;
        }

        @Override
        public byte read() throws IOException {
            byte[] b = read(1);
            return b[0];
        }

        @Override
        public byte[] read(int count) throws IOException {
            try {
                /*
                byte buffer[] = new byte[count];
                int read = 0;
                while (read < count) {
                    int len = min(count - read, 128);
                    byte[] b = mSerialPort.readBytes(len, 1000);
                    System.arraycopy(b, 0, buffer, read, len);
                    read += len;
                }
                */
                byte[] buffer = mSerialPort.readBytes(count, 3000);
                if (mDebug)
                    System.out.println("read bytes " + Hex.encodeHexString( buffer ));
                return buffer;
            } catch (Exception e) {
                throw new IOException(e);
            }
        }


        @Override
        void write(byte b) throws IOException {
            if (mDebug)
                System.out.println("write byte " + Byte.toUnsignedInt(b));
            try {
                mSerialPort.writeByte(b);
            } catch (SerialPortException e) {
                throw new IOException(e);
            }
        }

        @Override
        void write(byte[] bytes) throws IOException {
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

