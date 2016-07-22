# Fake Bluetooth GPS

Fake bluetooth GPS for Android. Allows a device to act as a GPS sending NMEA data over bluetooth. 

### Current version

v0.1.0

### Objectives

 - Create a fake bluetooth GPS for testing purposes (sending fake GPS data to devices expecting NMEA data over bluetooth).

### Easy peasy

```
LatLng target;

FakeBluetoothGPS fakeBluetoothGPS = new FakeBluetoothGPS();
fakeBluetoothGPS.start();

// ...

fakeBluetoothGPS.move(newPosition);

### Limitations

- Only RMC NMEA sentences are sent
- No speed calculation
- No checksum calculation

### License

Copyright 2016 ANTONIO CARLON

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.