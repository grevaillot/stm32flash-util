import jssc.SerialPort;
import jssc.SerialPortException;
import org.apache.commons.codec.binary.Hex;

import java.io.IOException;

public class jSTM32FlashUtil {
    public static void main(String[] args) {
        String port = "/dev/ttyUSB1";

        for (String arg : args)
            System.out.println(arg);

        System.out.println("jSTM32FlashUtil " + port);
        SerialPort sp = new SerialPort(port);

        try {
            sp.openPort();
            sp.setParams(SerialPort.BAUDRATE_115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_EVEN);
        } catch (SerialPortException e) {
            e.printStackTrace();
            return;
        }

        STM32Flasher flasher = new STM32Flasher(new JsscSerialIface(sp));

        try {
            flasher.connect();
            STM32Device d = flasher.getDevice();
            System.out.println("Connected to " + d);
            flasher.erase();
            //flasher.flashFirmware();
            flasher.dumpFirmware();
            flasher.reset();
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
                byte b[] = mSerialPort.readBytes(count, 1000);
                if (mDebug)
                    System.out.println("read bytes " + Hex.encodeHexString( b ));
                return b;
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

        public void setDebug(boolean mDebug) {
            this.mDebug = mDebug;
        }
    }


}


