# AIPMS

## 📌 Overview
This is a Java-based desktop application for managing users, tasks, products, and attendance. It supports data tracking and visualization using external chart libraries.

## 📁 Project Structure
```
.
├── build.bat                # Windows build script
├── data/                    # CSV and TXT data (users, tasks, products, etc.)
├── lib/                     # External libraries (JFreeChart, JUnit, etc.)
├── services/                # Compiled Java class files
```

## ✨ Features
- User account management
- Task and project assignment
- Product inventory system
- Attendance logging
- Login history tracking
- Data visualization with charts

## 🛠 Requirements
- Java Development Kit (JDK) 8+
- A Java IDE (IntelliJ, Eclipse, etc.)
- All JAR files in `lib/` are pre-included

## ▶️ How to Run
1. Open a terminal.
2. Run:
   ```bash
   build.bat
   ```
3. Then:
   ```bash
   java -cp "lib/*;." services.MainClass  # Replace 'MainClass' with the actual entry point
   ```

## 📚 Libraries Used
- **JUnit 4.11** – Unit testing framework
- **JFreeChart** – Chart drawing library
- **Orson Charts/PDF** – Advanced visualization tools
- **Hamcrest** – Matching/assertion framework

## 🧑‍💻 Author
Your Name Here

## 📄 License
This project is licensed under the MIT License.
