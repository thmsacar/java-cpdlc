# Java CPDLC
Java CPDLC is a multi-platform CPDLC Client for HoppieAcars. 
It is written on Java so you can use it on any platform (Windows/Linux/macOS).

![Screenshot of the Java CPDLC](src/resources/images/screenshot.png)

## Notes
- Currently, CPDLC messages cannot be sent as I have not yet implemented this feature totally. However, all types of messages can be fetched and replied (including CPDLC messages). This means TELEX messages can be sent and received with no problem.
- I am not planning to create a release before making a fully functional CPDLC client. So if you want to use JavaCPDLC, you should compile it on your machine.
## Important Note on Java 8
- Java CPDLC is coded on Java 8 (Java SDK 1.8) as I wanted it to be compatible on as many machine as possible. Therefore, it does not use HTTP Client, which was released after Java 11. Take this into account during compilation.

## Usage
1. Once compiled and run, you should have a login page. You need to put in your callsign and Hoppie ID. Click SAVE button
2. Thereafter, everything should be pretty straight forward.

- If your callsign is in green color, the connection with Hoppie server has established.
- If any connection problem occurs your callsign will become red.

## Contribution
As of now, the code doesn't have javadoc so it's a bit of mess. I will for sure add java doc and necessary comments to the code. 

However, any pull requests are still welcome. See below what I am currently working on. 

#### Features to implement
- Simbrief implementation for to fill auto pre-departure clearence. Backend part is completed and fully working.
- CPDLC menu and ATC connection. Currently, it is impossible so send any type of CPDLC message using the UI. Relevant UI pages should be created to: 
  1. Send LOGON request to ATC. Which will enable other CPDLC features only when logon request is accepted by a 'LOGON ACCEPTED' response.
  2. PDC request: Backend for this feature is completely finished. A UI page where user fills necessary information should be created. I am planning to add a space where it's possible to fetch Simbrief flight plan which will automatically fill these information.
  3. Send CPDLC requests and reports: These menus should be only activated if logged on to an ATC unit. Requests and reports should consist of certain templates where user fills necessary information. These templates should be created using ICAO documents, which is kind of a boring task. 
- CPDLC backend is working however frontend is not ready to implement this feature. I need to write CPDLC message templates to send classic CPDLC messages. 
- Finally: I need a logo for the application. I added a temporary logo to show up on taskbar, which is absolutely horrible.