# Implementation of exposure notification advertiser + scanner
This app can be used to advertise a known TEK and RPI which can be useful in the validation process.
It also scans for devices that broadcast exposure notification advertisements. If the TEK
of a particular device is known, the metadata containing the advertised tx power is decrypted.

## Setting the TEK
The TEK can currently only be set using a broadcast:

```aid
adb shell am broadcast -a nl.rijksoverheid.entoolkit.app.ACTION_SET_TEK -e hex 1f3e942ed13c63bfb45e046fb87a21f8 nl.rijksoverheid.entoolkit.app/.jobs.ImportTekReceiver
```
Where `1f3e942ed13c63bfb45e046fb87a21f8` is the hex encoded TEK. For the interval today's interval is assumed.
It's also possible to pass the base64 encoded version of the TEK data, use `base64` as the extra in that case.

## Known issues
* The advertiser has a fixed tx power which is not validated and is probably wrong, this means that apps
processing this advertisement will probably return an incorrect attenuation value.
