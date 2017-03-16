
.PHONE: all clean

all:
	make -C File_get
	make -C File_put
	make -C File_server
	make -C GenAscii

clean:
	make -C File_get clean
	make -C File_put clean
	make -C File_server clean
	make -C GenAscii clean

