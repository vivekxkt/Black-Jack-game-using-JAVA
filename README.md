# 🎴 Blackjack – JavaFX Edition

A polished, fully animated Blackjack game built using **JavaFX**.  
This project focuses on smooth card animations, a modern minimal UI, easy extensibility, and clean code organization.

---

## 🚀 Features

### ✅ Gameplay
- Classic Blackjack rules  
- Hit / Stand / Double  
- Dealer auto-play logic  
- Natural Blackjack (3:2 payout)  
- Hidden dealer card with flip animation  
- Smart Ace handling (1 or 11)

### ✅ UI & Visuals
- Modern glass-style HUD panels  
- Animated card dealing with slide + rotation  
- Glow highlight on the latest card  
- Dynamic stats chart (last 10 games)  
- Centered floating totals between dealer/player cards  
- Start screen with particles & fade transition  
- Smooth green table with vignette + gradient

### ✅ Betting System
- Balance, bet, and chips panel  
- Click chips to add bet  
- Right-click chips to remove  
- Doubling bet option  
- Clear bet button  
- Auto bet clamping when balance is low

### ✅ Stats Tracking
- Total games  
- Wins, losses, pushes  
- Best streak  
- Biggest win  
- Last 10 results graph

---


Here are some screenshots of gameplay : (Gameplay Preview)

<img width="1919" height="984" alt="image" src="https://github.com/user-attachments/assets/0acddd81-8907-4d44-8ee7-f348b8e5b0bc" />
<img width="1919" height="980" alt="Screenshot 2025-11-11 140305" src="https://github.com/user-attachments/assets/95187200-5e86-48aa-b097-c098781c6b2a" />
<img width="1919" height="976" alt="Screenshot 2025-11-11 140116" src="https://github.com/user-attachments/assets/c2cd5baf-d4e8-4293-8d19-7f843af255c1" />


## 📦 Project Structure

src/
│
├── blackjack/
│ ├── BlackJack.java # Main application + game logic + layout
│ ├── CardModel.java # Card, Deck, Suit, Rank models
│ ├── HUDComponents.java # Reusable HUD elements (card-style panels, labels, graph)
│
└── resources/
└── (optional images, icons, etc.)

## 🛠 Requirements

- **Java 17+**
- **JavaFX SDK** (controls, graphics, base modules)
- Any Java-friendly IDE:
  - IntelliJ IDEA  
  - Eclipse  
  - VS Code with JavaFX plugin  

---

## ✅ Running the Game

### **1. Download JavaFX**
Get the SDK from:  
https://openjfx.io/

### **2. Add VM Arguments (required)**  
If running manually:


🤝 Contributing

Pull requests are welcome!
If you’d like to add new features (split hands, insurance, improved animations, sounds), feel free to open an issue.
