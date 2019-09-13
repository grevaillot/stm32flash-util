import org.stm32flash.STM32Flasher;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class FullEraseTask extends Task {
    public FullEraseTask() {
        super();
    }

    @Override
    public boolean work(STM32Flasher flasher) throws IOException, TimeoutException {
        System.out.println("Erasing target firmware.");
        return flasher.eraseFirmware();
    }
}
