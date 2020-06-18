all: v_central

clean: v_central_clean

v_central:
	make --silent -C ./central

v_central_clean:
	make clean -C ./central
