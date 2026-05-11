# Java CPDLC
Java CPDLC is a multi-platform CPDLC Client for HoppieAcars. 
It is written on Java so you can use it on any platform (Windows/Linux/macOS).

![Screenshot of the Java CPDLC](src/resources/images/screenshot.png)

## Notes
- Currently, CPDLC reports cannot be sent as I have not yet implemented this feature totally. However, all other types of messages can be fetched and replied. Requests can be sent without problem.
## Important Note on Java 8
- Java CPDLC is coded on Java 8 (Java SDK 1.8) as I wanted it to be compatible on as many machine as possible. Therefore, it does not use HTTP Client, which was released after Java 11. Take this into account during compilation.

## ⚡ Quick Start (For Non-Developers)
If you don't want to deal with coding or building, just download the pre-compiled version:
1. Go to the **Releases** section on the right.
2. Download the latest release.
3. **Double-click** to run (Make sure you have Java 8 or higher installed).

## 🔨 Building & Running
The project uses **Maven** for the build process, so no manual library downloads are required. Make sure Maven is installed on your system

1. **Compile:** Open your terminal in the **main project folder** (where the `pom.xml` file is located) and run:
   ```bash
   mvn clean package
2. **Run:** After the build is complete, you can find the executable JAR file in the target folder. You can launch the application by double-clicking the JAR file or using the following command: 
   ```bash
   java -jar target/java-cpdlc-1.0.jar
   ```
   
## Usage
1. Once compiled and run, you should have a login page. You need to put in your callsign and Hoppie ID. Click SAVE button
2. Thereafter, everything should be pretty straight forward.

- If your callsign is in green color, the connection with Hoppie server has established.
- If any connection problem occurs your callsign will become red.

## Contribution
As of now, the code doesn't have javadoc so it's a bit of mess. I will for sure write javadoc and necessary comments to the code. 

However, any pull requests are still welcome. See below what I am currently working on. 

#### Features to implement
- Send CPDLC reports
- Send When can we? request
- Clean the code and structure

