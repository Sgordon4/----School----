Version 4
SymbolType CELL
RECTANGLE Normal -256 -192 240 192
TEXT 0 0 Center 2 LT
WINDOW 0 0 -64 Center 2
WINDOW 3 0 64 Center 2
SYMATTR Value LTC3725
SYMATTR Prefix X
SYMATTR SpiceModel LTC3725.sub
SYMATTR Value2 LTC3725
SYMATTR Description Single-Switch Forward Controller and Gate Driver\n\nNote:  Fault Transmit and Receive are not modeled as is not slave mode.
PIN -256 -112 LEFT 8
PINATTR PinName UVLO
PINATTR SpiceOrder 1
PIN -256 112 LEFT 8
PINATTR PinName SSFLT
PINATTR SpiceOrder 2
PIN -144 -192 TOP 8
PINATTR PinName Ndrv
PINATTR SpiceOrder 3
PIN 240 -80 RIGHT 8
PINATTR PinName FB/IN+
PINATTR SpiceOrder 4
PIN 240 80 RIGHT 8
PINATTR PinName FS/IN-
PINATTR SpiceOrder 5
PIN 144 192 BOTTOM 8
PINATTR PinName PGND
PINATTR SpiceOrder 6
PIN 0 -192 TOP 8
PINATTR PinName Gate
PINATTR SpiceOrder 7
PIN -256 0 LEFT 8
PINATTR PinName Vcc
PINATTR SpiceOrder 8
PIN -144 192 BOTTOM 8
PINATTR PinName VSLMT
PINATTR SpiceOrder 9
PIN 144 -192 TOP 8
PINATTR PinName IS
PINATTR SpiceOrder 10
PIN 0 192 BOTTOM 8
PINATTR PinName GND
PINATTR SpiceOrder 11
