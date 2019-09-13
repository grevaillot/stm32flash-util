import org.stm32flash.STM32Flasher;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class DoLockFlashReadoutTask extends Task {
    @Override
    public boolean work(STM32Flasher flasher) throws IOException, TimeoutException {
        System.out.println("Enabling Flash ReadOut protection for all memory");
        return flasher.lockFlashRead();
    }
}
