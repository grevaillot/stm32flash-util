# stm32flash-util

Java based simple "stm32flash" console tool using [stm32flash-lib](https://github.com/grevaillot/stm32flash-lib), a java based library to talk to stm32 UART bootloader following [AN3155](https://www.st.com/resource/en/application_note/cd00264342.pdf), allowing you to erase, write, verify and dump flash memory.

## Usage:
### Options:
    ./stm32flash-util
        -f path/to/file.bin     write file to target flash memory
        -F path/to/file.bin     write and verify file to target flash memory
        -p /dev/ttyUSB42        specify tty to use
        -b 115200               specify baudrate to use
        -r                      reset target after operation
        -d path/to/file.bin     dump target flash memory to file
        -e                      erase target flash memory
        -E 0x8000000:0x200      erase 0x200 bytes of flash memory from 0x8000000 
        -S 32k                  specify device flash size
        -V                      verbose
        -VV                     more verbose

### Example:
	./stm32flash-utl -b 115200 -p /dev/ttyUSB12 -F firmware.bin

## Notes:

See [stm32flash-lib](https://github.com/grevaillot/stm32flash-lib).
