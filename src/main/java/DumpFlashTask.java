import org.apache.commons.io.FileUtils;
import org.stm32flash.STM32Flasher;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class DumpFlashTask extends Task {
    private final String mFile;

    public DumpFlashTask(String file) {
        mFile = file;
    }

    @Override
    public boolean work(STM32Flasher flasher) throws IOException, TimeoutException {
        System.out.println("Dumping target firmware.");
        byte[] buffer = flasher.dumpFirmware();
        if (buffer == null) {
            System.err.println("Could not dump target firmware.");
            return false;
        }
        System.out.println("Saving firmware to " + mFile);
        FileUtils.writeByteArrayToFile(new File(mFile), buffer);
        return true;
    }
}
