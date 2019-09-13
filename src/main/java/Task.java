import org.stm32flash.STM32Flasher;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public abstract class Task {
    public abstract boolean work(STM32Flasher flasher) throws IOException, TimeoutException;
}

