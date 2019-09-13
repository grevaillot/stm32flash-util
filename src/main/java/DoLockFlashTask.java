import org.stm32flash.STM32Flasher;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static java.lang.System.exit;

public class DoLockFlashTask extends Task {
    @Override
    public boolean work(STM32Flasher flasher) throws IOException, TimeoutException {
        System.out.println("Enabling Flash Write protection for all memory");
        return flasher.lockFlashWrite();
    }
}
