import org.stm32flash.STM32Flasher;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.concurrent.TimeoutException;

public class PartialEraseTask extends Task {
    private Integer mStartAddress;
    private final Integer mLen;

    public PartialEraseTask(String flashRange) {
        String[] params = flashRange.split(":");

        if (params.length != 2) {
            System.err.println("bad partial erase parameter, expecting \"startaddress:length\" : " + flashRange);
            throw new InvalidParameterException();
        }

        mStartAddress = Integer.decode(params[0]);
        mLen = Integer.decode(params[1]);
    }

    @Override
    public boolean work(STM32Flasher flasher) throws IOException, TimeoutException {

        if (mStartAddress == 0) {
            mStartAddress = flasher.getDevice().getFlashStart();
        } else if (mStartAddress < flasher.getDevice().getFlashStart()) {
            System.err.println("bad partial erase parameter, start address is below flash start.");
            return false;
        }

        if (mStartAddress + mLen > flasher.getDevice().getFlashStart() + flasher.getDevice().getFlashSize()) {
            System.err.println("bad partial erase parameter, end address is after flash end.");
            return false;
        }

        System.out.println("Erasing target from 0x" + Integer.toHexString(mStartAddress) + " to 0x" + Integer.toHexString(mStartAddress + mLen));

        if (!flasher.erase(mStartAddress, mLen)) {
            System.err.println("Could not erase flash.");
            return false;
        }

        return true;
    }
}
