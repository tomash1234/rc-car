import serial
import sys
import glob

import serial.tools.list_ports

available_ports = serial.tools.list_ports.comports()
for port, desc, hwid in available_ports:
    print(f"Port: {port}, Description: {desc}, Hardware ID: {hwid}")


ser = serial.Serial('COM4', 9600)

try:
    # Open the serial port
    ser.open()

    # Check if the serial port is open
    if ser.is_open:
        print("Serial port is open.")

        # Read data from the serial port
        while True:
            data = ser.readline().decode().strip()  # Read a line of data from the serial port
            print("Received data:", data)

except serial.SerialException as e:
    print("Error: Could not open the serial port. " + str(e))

finally:
    # Close the serial port
    ser.close()
    print("Serial port is closed.")