import org.apache.commons.codec.binary.Hex;
import org.stm32flash.STM32Firmware;
import org.stm32flash.STM32Flasher;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class FlashFileTask extends Task {
    private final String mFile;
    private final STM32Firmware mFw;
    private final boolean mVerify;

    public FlashFileTask(String file, boolean verify) throws Exception {
        mFile = file;
        mFw = new STM32Firmware(file);
        mVerify = verify;

        System.out.println("Loaded " + file + ", md5=" + Hex.encodeHexString( mFw.getChecksum() ));
    }

    @Override
    public boolean work(STM32Flasher flasher) throws IOException, TimeoutException {
        System.out.println("Flashing " + mFile + " (" + mFw.getSize() + "b) to target.");

        if (!flasher.flashFirmware(mFw.getBuffer(), STM32Flasher.EraseMode.Partial, mVerify)) {
            System.err.println("Firmware flashing failed.");
            return false;
        }

        System.out.println("Firmware was flashed successfully.");
        return true;
    }
}
