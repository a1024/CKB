cmake_minimum_required(VERSION 3.4.1)

# now build app's shared lib
# set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")

add_library(
	ckb SHARED
	array.c
	ckb.c
	default_config.cpp
)

# add lib dependencies
target_link_libraries(
	ckb
	android
	log
	#EGL
	#GLESv2
)
