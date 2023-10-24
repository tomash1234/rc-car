# Hardware

Componets needed:
- RC Car (Rock Crawler Reely 1:18) with a controller
  - <img src="../img/car.png" width="300">
- ESP8266 (WEMOS D1 R2 WiFi)
- 4x Relay 3.3V
- 4x NPN Transistors
- Wires

## Hacking Remote Controller with Wemos / Arduino
1. Extract battery and board from the remote controller
    -  <img src="../img/control_extraction.png" width="300">
    
2. Locate buttons on board from RC Controller. There are buttons for each movement: left/right, forward / backward.
    - <img src="../img/chip.png" width="300">
3. Create circuits with relays and transistors in order to simulate clicking on controller's buttons.
    - <img src="../img/circuit.png" width="500">
4. Connect wires from each relay to a controller button
   - <img src="../img/chip_back.png" width="300">
5. Connect wires to Wemos as described in the schema above
6. Done
   - <img src="../img/completed_circuit.png" width="500">

## Car modifications