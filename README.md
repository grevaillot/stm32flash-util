# stm32flash-util

Java based simple "stm2flash" console tool using [stm32flash-lib](https://github.com/grevaillot/stm32flash-lib), a java based library to talk to stm32 UART bootloader following [AN3155](https://www.st.com/resource/en/application_note/cd00264342.pdf)

## Usage:
### Options:
    ./stm32flash-util
        -e                      erase target flash memory
        -f "path/to/file.bin"   flash file to target flash memory
        -r                      reset target after operation
        -v                      verify flash content while flashing             
        -p /dev/ttyUSB42        specify tty to use
        -b 115200               specify baudrate to use
        -d "path/to/file.bin"	dump target flash memory to file
        -V                      verbose
        -VV                     more verbose

### Example:
	./stm32flash-utl -p /dev/ttyUSB12 -e -f firmware.bin  -v

