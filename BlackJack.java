import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.effect.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.transform.Rotate;

import blackjack.CardModel.*;
import blackjack.HUDComponents;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class BlackJack extends Application {

    // ===== Game Model =====
    private Deck deck;
    private final List<Card> playerHand = new ArrayList<>();
    private final List<Card> dealerHand = new ArrayList<>();

    // ===== UI containers =====
    private Pane tablePane;
    private StackPane rootStack;

    private VBox hudTopLeft;
    private VBox hudTopRight;
    private VBox hudBottomRight;
    private VBox chipRack;
    private HBox controlsBar;

    // HUD labels
    private Label balanceLabel, betLabel, statusLabel;
    private Label dealerTotalLabel, playerTotalLabel;
    private Label gamesLabel, winsLabel, lossesLabel, pushesLabel, streakLabel, biggestWinLabel;

    // Buttons
    private Button hitBtn, standBtn, doubleBtn, playAgainBtn, clearBetBtn;

    private Canvas graphCanvas;

    // ===== Values =====
    private int balance = 1000;
    private int bet = 100;
    private boolean dealerRevealed = false;
    private boolean roundOver = false;

    private int playerTotal = 0;
    private int dealerTotal = 0;

    // ===== Stats =====
    private int gamesPlayed = 0;
    private int wins = 0;
    private int losses = 0;
    private int pushes = 0;
    private int bestStreak = 0;
    private int currentStreak = 0;
    private int biggestWin = 0;

    private final Deque<Integer> last10 = new ArrayDeque<>();

    // ===== Card Nodes =====
    private final List<Node> playerNodes = new ArrayList<>();
    private final List<Node> dealerNodes = new ArrayList<>();
    private Node lastGlow;

    private Timeline dealerLoop;

    // Card sizes
    private static final double CARD_W = 130;
    private static final double CARD_H = 180;

    private double deckX = 28;
    private double deckY = 28;

    @Override
    public void start(Stage stage) {

        // ===== Table setup =====
        tablePane = new Pane();
        tablePane.setMinSize(1100, 700);
        tablePane.setRotationAxis(Rotate.X_AXIS);
        tablePane.setRotate(2.5);

        tablePane.setBackground(new Background(
                new BackgroundFill(Color.web("#0A5E20"), CornerRadii.EMPTY, Insets.EMPTY)
        ));

        Region felt = new Region();
        felt.prefWidthProperty().bind(tablePane.widthProperty());
        felt.prefHeightProperty().bind(tablePane.heightProperty());
        felt.setBackground(new Background(new BackgroundFill(
                new LinearGradient(0,0,1,1,true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#085A1E")),
                        new Stop(1, Color.web("#126E23"))),
                CornerRadii.EMPTY, Insets.EMPTY)));

        Region vignette = new Region();
        vignette.prefWidthProperty().bind(tablePane.widthProperty());
        vignette.prefHeightProperty().bind(tablePane.heightProperty());
        vignette.setBackground(new Background(new BackgroundFill(
                new RadialGradient(0,0,0.5,0.5,1,true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.color(0,0,0,0)),
                        new Stop(1, Color.color(0,0,0,0.25))),
                CornerRadii.EMPTY, Insets.EMPTY)));

        tablePane.getChildren().addAll(felt, vignette);
        drawDeck();

        // ===== Main base =====
        BorderPane base = new BorderPane();
        base.setCenter(tablePane);

        // ===== HUD =====
        buildHUD();

        AnchorPane overlay = new AnchorPane(hudTopLeft, hudTopRight, hudBottomRight, chipRack, controlsBar);
        AnchorPane.setTopAnchor(hudTopLeft, 340.0);
        AnchorPane.setLeftAnchor(hudTopLeft, 20.0);

        AnchorPane.setTopAnchor(hudTopRight, 20.0);
        AnchorPane.setRightAnchor(hudTopRight, 20.0);

        AnchorPane.setBottomAnchor(hudBottomRight, 20.0);
        AnchorPane.setRightAnchor(hudBottomRight, 20.0);

        AnchorPane.setTopAnchor(chipRack, 580.0);
        AnchorPane.setLeftAnchor(chipRack, 20.0);

        AnchorPane.setBottomAnchor(controlsBar, 20.0);
        AnchorPane.setLeftAnchor(controlsBar, 0.0);
        AnchorPane.setRightAnchor(controlsBar, 0.0);

        StackPane layered = new StackPane(base, overlay);

        // ===== Start screen =====
        StackPane startScreen = buildStartScreen(() ->
                Platform.runLater(this::onPlayAgain)
        );

        rootStack = new StackPane(layered, startScreen);

        Scene scene = new Scene(rootStack, 1250, 760);
        stage.setTitle("Blackjack (JavaFX)");
        stage.setScene(scene);
        stage.show();

        Platform.runLater(this::layoutAllCards);

        tablePane.widthProperty().addListener((a,b,c)-> layoutAllCards());
        tablePane.heightProperty().addListener((a,b,c)-> layoutAllCards());
    }

    // ===============================================================================================
    // HUD + UI
    // ===============================================================================================

    private void buildHUD() {

    // -------- Top Left (Session) --------
    hudTopLeft = HUDComponents.glassCard("Session");

    balanceLabel = HUDComponents.hudValue("Balance: ₹" + balance);
    betLabel = HUDComponents.hudValue("Bet: ₹" + bet);
    statusLabel = HUDComponents.hudValue("Click Play to begin");

    hudTopLeft.getChildren().addAll(balanceLabel, betLabel, statusLabel);


    // -------- Top Right (Totals) --------
    hudTopRight = HUDComponents.glassCard("Totals");

    dealerTotalLabel = HUDComponents.statHeader("Dealer: 0");
    playerTotalLabel = HUDComponents.statHeader("You: 0");

    hudTopRight.getChildren().addAll(dealerTotalLabel, playerTotalLabel);


    // -------- Bottom Right (Stats) --------
    hudBottomRight = HUDComponents.glassCard("Stats");

    gamesLabel = HUDComponents.statLabel("Games: 0");
    winsLabel = HUDComponents.statLabel("Wins: 0");
    lossesLabel = HUDComponents.statLabel("Losses: 0");
    pushesLabel = HUDComponents.statLabel("Pushes: 0");
    streakLabel = HUDComponents.statLabel("Best Streak: 0");
    biggestWinLabel = HUDComponents.statLabel("Biggest Win: ₹0");

    graphCanvas = HUDComponents.makeGraphCanvas();

    hudBottomRight.getChildren().addAll(
            new HBox(16, gamesLabel, winsLabel),
            new HBox(16, lossesLabel, pushesLabel),
            new HBox(16, streakLabel, biggestWinLabel),
            HUDComponents.statLabel("Last 10 (win=↑ loss=↓)"),
            graphCanvas
    );


    // -------- Bottom Left (Chips) --------
        chipRack = new VBox(10);          // <-- CHANGED from HBox → VBox
        chipRack.setAlignment(Pos.CENTER_LEFT);
        chipRack.setPadding(new Insets(12));
        chipRack.setBackground(new Background(new BackgroundFill(
                Color.rgb(18,22,25,0.65), new CornerRadii(16), Insets.EMPTY)));
        chipRack.setBorder(new Border(new BorderStroke(
                Color.rgb(255,255,255,0.15),
                BorderStrokeStyle.SOLID, new CornerRadii(16), new BorderWidths(1))));
        chipRack.setEffect(new DropShadow(18, Color.color(0,0,0,0.55)));

        // Chips to display (₹10 removed)
        int[] row1 = {50, 100};
        int[] row2 = {500, 1000};

        // First row
        HBox rowBox1 = new HBox(12);
        rowBox1.setAlignment(Pos.CENTER_LEFT);
        for(int amount : row1){
            rowBox1.getChildren().add(makeChip(amount));
        }

        // Second row
        HBox rowBox2 = new HBox(12);
        rowBox2.setAlignment(Pos.CENTER_LEFT);
        for(int amount : row2){
            rowBox2.getChildren().add(makeChip(amount));
        }

        // Clear Bet button (third row)
        clearBetBtn = makeButton("Clear Bet");
        clearBetBtn.setPrefWidth(150);
        clearBetBtn.setOnAction(e -> {
            bet = 0;
            betLabel.setText("Bet: ₹0");
        });

        // Add rows into vertical panel
        chipRack.getChildren().addAll(rowBox1, rowBox2, clearBetBtn);



    // -------- Controls --------
    controlsBar = new HBox(16);
    controlsBar.setAlignment(Pos.CENTER);
    controlsBar.setPadding(new Insets(8));

    hitBtn = makeButton("Hit");
    standBtn = makeButton("Stand");
    doubleBtn = makeButton("Double");
    playAgainBtn = makeButton("Play");

    controlsBar.getChildren().addAll(hitBtn, standBtn, doubleBtn, playAgainBtn);

    hitBtn.setOnAction(e -> onHit());
    standBtn.setOnAction(e -> onStand());
    doubleBtn.setOnAction(e -> onDouble());
    playAgainBtn.setOnAction(e -> onPlayAgain());

    setButtonsEnabled(false);
    playAgainBtn.setDisable(false);
}


    private Button makeButton(String text){
        Button b = new Button(text);
        b.setPrefWidth(140);
        b.setPrefHeight(46);
        b.setFont(Font.font("Segoe UI", FontWeight.BOLD, 17));
        b.setTextFill(Color.WHITE);
        b.setBackground(new Background(new BackgroundFill(
                Color.web("#007E63"), new CornerRadii(16), Insets.EMPTY)));
        b.setOnMouseEntered(e ->
                b.setBackground(new Background(new BackgroundFill(
                        Color.web("#009974"), new CornerRadii(16), Insets.EMPTY))));
        b.setOnMouseExited(e ->
                b.setBackground(new Background(new BackgroundFill(
                        Color.web("#007E63"), new CornerRadii(16), Insets.EMPTY))));
        return b;
    }

    private Button makeChip(int amount){
        Button b = new Button("₹"+amount);
        b.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        b.setTextFill(Color.WHITE);
        b.setBackground(new Background(new BackgroundFill(
                Color.web("#1F6E5E"), new CornerRadii(18), Insets.EMPTY)));
        b.setPadding(new Insets(10, 16, 10, 16));
        b.setEffect(new DropShadow(10, Color.color(0,0,0,0.45)));

        b.setOnMouseClicked(e -> {
            if(e.getButton() == MouseButton.PRIMARY){
                int maxAdd = balance - bet;
                int add = Math.min(amount, maxAdd);
                bet += add;
            } else if(e.getButton() == MouseButton.SECONDARY){
                bet = Math.max(0, bet - amount);
            }
            betLabel.setText("Bet: ₹" + bet);
        });

        return b;
    }

    // ===============================================================================================
    // START SCREEN
    // ===============================================================================================

    private StackPane buildStartScreen(Runnable onStart) {

        StackPane splash = new StackPane();
        splash.setStyle("-fx-background-color: linear-gradient(to bottom, #002615, #014A2A);");

        // --- Deep gradient background ---
        Rectangle bg = new Rectangle();
        bg.widthProperty().bind(splash.widthProperty());
        bg.heightProperty().bind(splash.heightProperty());
        bg.setFill(new LinearGradient(
                0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#00351F")),
                new Stop(1, Color.web("#00150C"))
        ));

        // --- Soft radial glow in the center ---
        Rectangle glow = new Rectangle();
        glow.widthProperty().bind(splash.widthProperty());
        glow.heightProperty().bind(splash.heightProperty());
        glow.setFill(new RadialGradient(
                0, 0,
                0.5, 0.5,
                0.45,
                true,
                CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(0, 0.5, 0.2, 0.28)),
                new Stop(1, Color.color(0, 0, 0, 0))
        ));

        // --- Floating particles (slower, smoother) ---
        Group particles = new Group();
        for (int i = 0; i < 38; i++) {
            Circle dot = new Circle(ThreadLocalRandom.current().nextDouble(1.2, 2.8));
            dot.setFill(Color.color(1, 1, 1, ThreadLocalRandom.current().nextDouble(0.03, 0.08)));
            dot.setEffect(new BoxBlur(3, 3, 2));

            dot.setTranslateX(ThreadLocalRandom.current().nextDouble(0, 1300));
            dot.setTranslateY(ThreadLocalRandom.current().nextDouble(0, 800));

            TranslateTransition drift = new TranslateTransition(
                    Duration.seconds(ThreadLocalRandom.current().nextDouble(12, 22)), dot);
            drift.setByY(-ThreadLocalRandom.current().nextDouble(60, 140));
            drift.setByX(ThreadLocalRandom.current().nextDouble(-20, 20));
            drift.setAutoReverse(true);
            drift.setCycleCount(Animation.INDEFINITE);
            drift.play();

            particles.getChildren().add(dot);
        }

        Label clubIcon = new Label("♣");
        clubIcon.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 110));
        clubIcon.setTextFill(Color.WHITE);
        clubIcon.setEffect(new DropShadow(35, Color.BLACK));


        // --- Title ---
        Label title = new Label("BLACKJACK");
        title.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 70));
        title.setTextFill(Color.WHITE);
        title.setEffect(new DropShadow(45, Color.BLACK));

        // Zoom-in animation for title
        ScaleTransition zoom = new ScaleTransition(Duration.seconds(1.1), title);
        zoom.setFromX(0.88);
        zoom.setFromY(0.88);
        zoom.setToX(1);
        zoom.setToY(1);
        zoom.setInterpolator(Interpolator.EASE_OUT);
        zoom.play();

        // --- Subtitle ---
        Label subtitle = new Label("A clean & animated JavaFX Blackjack game");
        subtitle.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 20));
        subtitle.setTextFill(Color.color(1, 1, 1, 0.75));

        // --- Start Button ---
        Button startBtn = makeButton("Start Game");
        startBtn.setPrefWidth(260);
        startBtn.setPrefHeight(65);
        startBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));

        VBox box = new VBox(20,clubIcon, title, subtitle, startBtn);
        box.setAlignment(Pos.CENTER);

        splash.getChildren().addAll(bg, glow, particles, box);

        // Fade out the entire screen when button is clicked
        startBtn.setOnAction(e -> {
            FadeTransition fade = new FadeTransition(Duration.millis(650), splash);
            fade.setToValue(0);
            fade.setInterpolator(Interpolator.EASE_BOTH);

            fade.setOnFinished(ev -> {
                rootStack.getChildren().remove(splash);
                if (onStart != null) onStart.run();
            });

            fade.play();
        });

        return splash;
    }


    // ===============================================================================================
    // GAME FLOW
    // ===============================================================================================

    private void onPlayAgain(){
        if(balance <= 0){
            if(!confirm("Balance is 0. Refill ₹1000?")) return;
            balance = 1000;
            balanceLabel.setText("Balance: ₹1000");
        }
        setButtonsEnabled(false);
        startRound();
    }

    private void startRound() {
    stopDealerLoop();

    roundOver = false;
    dealerRevealed = false;

    playerHand.clear();
    dealerHand.clear();
    playerNodes.clear();
    dealerNodes.clear();

    tablePane.getChildren().removeIf(n -> "CARD".equals(n.getUserData()));

    deck = new Deck();

    if (bet > balance) bet = balance;
    if (bet <= 0) bet = Math.min(100, balance);

    balance -= bet;
    balanceLabel.setText("Balance: ₹" + balance);
    betLabel.setText("Bet: ₹" + bet);

    statusLabel.setText("Dealing...");

    // --------- Deal order: P1 → D1 → P2 → D2(hidden) ---------

    dealTo(playerHand, playerNodes, true, false, () -> {

        dealTo(dealerHand, dealerNodes, true, false, () -> {

            dealTo(playerHand, playerNodes, true, false, () -> {

                dealTo(dealerHand, dealerNodes, false, true, () -> {

                    computeTotals();
                    checkNaturalBlackjack();

                    if (!roundOver) {
                        setButtonsEnabled(true);
                        statusLabel.setText("Your move: Hit / Stand / Double");
                    }

                });

            });

        });

    });
}


    private void onHit(){
        if(roundOver) return;
        dealTo(playerHand, playerNodes, true, false, () -> {
            computeTotals();
            if(playerTotal > 21){
                dealerRevealed = true;
                revealDealer(() -> endRound("You busted. Dealer wins.", false,false));
            }
        });
    }

    private void onStand(){
        if(roundOver) return;
        dealerRevealed = true;
        setButtonsEnabled(false);
        revealDealer(this::dealerTurn);
    }

    private void onDouble(){
        if(roundOver) return;
        if(balance < bet){
            alert("Not enough balance to double.");
            return;
        }

        balance -= bet;
        bet *= 2;

        balanceLabel.setText("Balance: ₹"+balance);
        betLabel.setText("Bet: ₹"+bet);

        setButtonsEnabled(false);

        dealTo(playerHand, playerNodes, true, false, () -> {
            computeTotals();
            if(playerTotal > 21){
                dealerRevealed = true;
                revealDealer(() -> endRound("You busted after doubling.", false,false));
            } else {
                dealerRevealed = true;
                revealDealer(this::dealerTurn);
            }
        });
    }

    private void dealerTurn(){
        stopDealerLoop();
        dealerLoop = new Timeline(new KeyFrame(Duration.millis(380), e -> {
            if(roundOver){ stopDealerLoop(); return; }
            computeTotals();
            if(dealerTotal < 17){
                dealTo(dealerHand, dealerNodes, true, false, null);
            } else {
                stopDealerLoop();
                decideOutcome();
            }
        }));
        dealerLoop.setCycleCount(Animation.INDEFINITE);
        dealerLoop.play();
    }

    private void stopDealerLoop(){
        if(dealerLoop != null){
            dealerLoop.stop();
            dealerLoop = null;
        }
    }

    private void decideOutcome(){
        if(roundOver) return;

        boolean pBust = playerTotal > 21;
        boolean dBust = dealerTotal > 21;

        if(pBust && dBust){ endRound("Both busted. Push.", false,true); return; }
        if(pBust){ endRound("You busted. Dealer wins.", false,false); return; }
        if(dBust){ endRound("Dealer busted! You win!", true,false); return; }

        if(playerTotal > dealerTotal) endRound("You win!", true,false);
        else if(dealerTotal > playerTotal) endRound("Dealer wins.", false,false);
        else endRound("Push.", false,true);
    }

    private void endRound(String message, boolean playerWon, boolean push){
        if(roundOver) return;
        roundOver = true;

        statusLabel.setText(message);

        int winAmount = 0;

        if(push){
            balance += bet;
            pushes++;
            currentStreak = 0;
            addResult(0);

        } else if(playerWon){
            balance += bet * 2;
            wins++;
            currentStreak++;
            winAmount = bet;
            bestStreak = Math.max(bestStreak, currentStreak);
            biggestWin = Math.max(biggestWin, winAmount);
            addResult(1);

        } else {
            losses++;
            currentStreak = 0;
            addResult(-1);
        }

        gamesPlayed++;
        updateStatsHUD();

        balanceLabel.setText("Balance: ₹"+balance);

        setButtonsEnabled(false);
        playAgainBtn.setDisable(false);
    }

    // ===============================================================================================
    // STATS
    // ===============================================================================================

    private void updateStatsHUD(){
        gamesLabel.setText("Games: " + gamesPlayed);
        winsLabel.setText("Wins: " + wins);
        lossesLabel.setText("Losses: " + losses);
        pushesLabel.setText("Pushes: " + pushes);
        streakLabel.setText("Best Streak: " + bestStreak);
        biggestWinLabel.setText("Biggest Win: ₹" + biggestWin);
    }

    private void addResult(int r){
        last10.addLast(r);
        if(last10.size() > 10) last10.removeFirst();
        drawGraph();
    }

    private void drawGraph(){
        GraphicsContext g = graphCanvas.getGraphicsContext2D();
        double w = graphCanvas.getWidth();
        double h = graphCanvas.getHeight();

        g.setFill(Color.color(1,1,1,0.08));
        g.fillRect(0,0,w,h);

        g.setStroke(Color.WHITE);
        g.setLineWidth(1.5);
        g.setGlobalAlpha(0.25);
        g.strokeLine(0, h*0.75, w, h*0.75);
        g.strokeLine(0, h*0.5, w, h*0.5);
        g.strokeLine(0, h*0.25, w, h*0.25);
        g.setGlobalAlpha(1.0);

        if(last10.isEmpty()) return;

        int n = last10.size();
        double step = w / Math.max(1, n - 1);

        Iterator<Integer> it = last10.iterator();
        double px = 0, py = 0;
        boolean first = true;

        for(int i=0; i<n; i++){
            int val = it.next();
            double y = (val == -1 ? h*0.75 : val == 0 ? h*0.5 : h*0.25);
            double x = i * step;

            if(first){
                px = x; py = y;
                first = false;
            } else {
                g.strokeLine(px, py, x, y);
                px = x; py = y;
            }

            g.fillOval(x-3,y-3,6,6);
        }
    }

    // ===============================================================================================
    // CARDS + ANIMATION
    // ===============================================================================================

    private void dealTo(List<Card> hand, List<Node> nodes, boolean faceUp, boolean hideDealerSecond, Runnable after){
        Card card = deck.draw();
        hand.add(card);

        boolean faceDown = (hand == dealerHand && !dealerRevealed && hand.size() == 2 && hideDealerSecond);

        Node cardNode = createCardNode(card, !faceDown);
        cardNode.setUserData("CARD");
        nodes.add(cardNode);

        cardNode.setLayoutX(deckX);
        cardNode.setLayoutY(deckY);
        tablePane.getChildren().add(cardNode);

        Platform.runLater(() -> {
            int count = hand.size();

            double totalW = Math.max(1, tablePane.getWidth());
            double margin = 40;
            double usable = totalW - 2*margin;
            double spacing = (count <= 1) ? 0 : Math.min(160, Math.max(60,(usable - CARD_W)/(count - 1)));
            double groupWidth = (count - 1) * spacing + CARD_W;
            double startX = (totalW - groupWidth)/2.0;

            int index = count - 1;
            double tx = startX + index*spacing;
            double ty = (hand == playerHand) ? (tablePane.getHeight()-320) : 120;

            TranslateTransition tt = new TranslateTransition(Duration.millis(340), cardNode);
            tt.setToX(tx - deckX);
            tt.setToY(ty - deckY);
            tt.setInterpolator(Interpolator.EASE_OUT);

            RotateTransition rt = new RotateTransition(Duration.millis(260), cardNode);
            rt.setByAngle(ThreadLocalRandom.current().nextDouble(-6,6));

            ParallelTransition pt = new ParallelTransition(tt, rt);

            pt.setOnFinished(ev -> {
                cardNode.setTranslateX(0);
                cardNode.setTranslateY(0);
                cardNode.setLayoutX(tx);
                cardNode.setLayoutY(ty);
                applyGlow(cardNode);
                if(after != null) after.run();
            });

            pt.play();
        });
    }

    private Node createCardNode(Card c, boolean faceUp){
        StackPane sp = new StackPane();
        sp.setPrefSize(CARD_W, CARD_H);

        Rectangle rect = new Rectangle(CARD_W, CARD_H);
        rect.setArcWidth(18); rect.setArcHeight(18);
        rect.setStroke(Color.color(0,0,0,0.75));
        rect.setStrokeWidth(2);

        if(faceUp){
            rect.setFill(Color.WHITE);

            Rectangle shine = new Rectangle(CARD_W - 6, 36);
            shine.setArcWidth(14); shine.setArcHeight(14);
            shine.setTranslateY(CARD_H/2 - 22);
            shine.setFill(new LinearGradient(0,0,0,1,true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.color(0,0,0,0)),
                    new Stop(1, Color.color(0,0,0,0.22))));

            Text tl = new Text(c.rank.label);
            tl.setFont(Font.font("SansSerif", FontWeight.BOLD, 22));
            tl.setTranslateX(-CARD_W/2 + 16);
            tl.setTranslateY(-CARD_H/2 + 28);

            Text sl = new Text(c.suit.symbol);
            sl.setFont(Font.font("SansSerif", FontWeight.BOLD, 22));
            sl.setTranslateX(-CARD_W/2 + 16);
            sl.setTranslateY(-CARD_H/2 + 54);

            Text big = new Text(c.suit.symbol);
            big.setFont(Font.font("SansSerif", FontWeight.BOLD, 48));

            Paint color = c.suit.isRed() ? Color.web("#C21E1E") : Color.BLACK;
            tl.setFill(color);
            sl.setFill(color);
            big.setFill(color);

            sp.getChildren().addAll(rect, shine, tl, sl, big);

        } else {
            rect.setFill(Color.web("#1E3278"));
            rect.setStroke(Color.WHITE);

            Rectangle pattern = new Rectangle(CARD_W - 24, CARD_H - 24);
            pattern.setArcWidth(14);
            pattern.setArcHeight(14);
            pattern.setFill(Color.color(1,1,1,0.15));

            sp.getChildren().addAll(rect, pattern);
        }

        return sp;
    }

    private void revealDealer(Runnable after){
        if(dealerNodes.size() >= 2){
            Node n = dealerNodes.get(1);

            ScaleTransition s1 = new ScaleTransition(Duration.millis(120), n);
            s1.setToX(0);

            ScaleTransition s2 = new ScaleTransition(Duration.millis(120), n);
            s2.setToX(1);

            s1.setOnFinished(e -> setCardFaceUp(n, dealerHand.get(1)));

            SequentialTransition seq = new SequentialTransition(s1, s2);
            seq.setOnFinished(e -> {
                computeTotals();
                if(after != null) after.run();
            });

            seq.play();

        } else {
            if(after != null) after.run();
        }
    }

    private void setCardFaceUp(Node node, Card c){
        if(!(node instanceof StackPane sp)) return;

        sp.getChildren().clear();

        Rectangle rect = new Rectangle(CARD_W, CARD_H);
        rect.setArcWidth(18);
        rect.setArcHeight(18);
        rect.setFill(Color.WHITE);
        rect.setStroke(Color.color(0,0,0,0.75));

        Rectangle shine = new Rectangle(CARD_W - 6, 36);
        shine.setArcWidth(14);
        shine.setArcHeight(14);
        shine.setTranslateY(CARD_H/2 - 22);
        shine.setFill(new LinearGradient(0,0,0,1,true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(0,0,0,0)),
                new Stop(1, Color.color(0,0,0,0.22))));

        Text tl = new Text(c.rank.label);
        tl.setFont(Font.font("SansSerif", FontWeight.BOLD, 22));
        tl.setTranslateX(-CARD_W/2 + 16);
        tl.setTranslateY(-CARD_H/2 + 28);

        Text sl = new Text(c.suit.symbol);
        sl.setFont(Font.font("SansSerif", FontWeight.BOLD, 22));
        sl.setTranslateX(-CARD_W/2 + 16);
        sl.setTranslateY(-CARD_H/2 + 54);

        Text big = new Text(c.suit.symbol);
        big.setFont(Font.font("SansSerif", FontWeight.BOLD, 48));

        Paint color = c.suit.isRed() ? Color.web("#C21E1E") : Color.BLACK;
        tl.setFill(color);
        sl.setFill(color);
        big.setFill(color);

        sp.getChildren().addAll(rect, shine, tl, sl, big);
    }

    private void applyGlow(Node n){
        if(lastGlow != null) lastGlow.setEffect(null);

        DropShadow glow = new DropShadow();
        glow.setRadius(24);
        glow.setSpread(0.12);
        glow.setColor(Color.color(1,1,0.7,0.8));

        n.setEffect(glow);
        lastGlow = n;
    }

    // ===============================================================================================
    // TOTALS + LAYOUT
    // ===============================================================================================

    private void computeTotals(){
        playerTotal = handValue(playerHand);
        dealerTotal = handValue(dealerHand);

        int shownDealer = dealerRevealed ?
                dealerTotal :
                (dealerHand.isEmpty() ? 0 : cardValue(dealerHand.get(0)));

        dealerTotalLabel.setText("Dealer: " + shownDealer);
        playerTotalLabel.setText("You: " + playerTotal);

        layoutAllCards();

        // ✅ ADD THIS
        centerTotalsBetweenCards();
    }


   private void centerTotalsBetweenCards() {

        tablePane.getChildren().removeIf(n -> "CENTER_TOTAL".equals(n.getUserData()));

        double w = tablePane.getWidth();

        // ===== EASY VALUES YOU CAN CHANGE =====
        double centerX = w / 2 + 10;   // ← change this to move left / right
        double dealerY = 370;         // ← dealer total vertical position
        double playerY = 430;         // ← your total vertical position
        // =====================================

        int shownDealer = dealerRevealed ?
                dealerTotal :
                (dealerHand.isEmpty() ? 0 : cardValue(dealerHand.get(0)));

        // Dealer text
        Text dealerText = new Text("Dealer: " + shownDealer);
        dealerText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 26));
        dealerText.setFill(Color.WHITE);
        dealerText.setUserData("CENTER_TOTAL");

        dealerText.setLayoutX(centerX - dealerText.getBoundsInLocal().getWidth()/2);
        dealerText.setLayoutY(dealerY);


        // Player text
        Text playerText = new Text("You: " + playerTotal);
        playerText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 26));
        playerText.setFill(Color.WHITE);
        playerText.setUserData("CENTER_TOTAL");

        playerText.setLayoutX(centerX - playerText.getBoundsInLocal().getWidth()/2);
        playerText.setLayoutY(playerY);

        tablePane.getChildren().addAll(dealerText, playerText);
    }




    private static int handValue(List<Card> hand){
        int sum = 0;
        int aces = 0;

        for(Card c : hand){
            sum += cardValue(c);
            if(c.rank == Rank.ACE) aces++;
        }

        while(sum > 21 && aces > 0){
            sum -= 10;
            aces--;
        }

        return sum;
    }

    private static int cardValue(Card c){
        if(c.rank == Rank.ACE) return 11;
        if(c.rank.numeric >= 10) return 10;
        return c.rank.numeric;
    }

    private void checkNaturalBlackjack(){
        if(playerTotal == 21){
            dealerRevealed = true;

            revealDealer(() -> {
                if(dealerTotal == 21){
                    endRound("Push! Both have 21.", false,true);
                } else {
                    roundOver = true;
                    balance += bet + Math.round(1.5 * bet);
                    balanceLabel.setText("Balance: ₹"+balance);

                    statusLabel.setText("Blackjack! You win (3:2)");

                    wins++;
                    gamesPlayed++;
                    currentStreak++;
                    bestStreak = Math.max(bestStreak, currentStreak);
                    biggestWin = Math.max(biggestWin, (int)Math.round(1.5 * bet));

                    addResult(1);
                    updateStatsHUD();

                    setButtonsEnabled(false);
                    playAgainBtn.setDisable(false);
                }
            });
        }
    }

    private void layoutAllCards(){
        layoutHand(dealerHand, dealerNodes, false, 120);
        layoutHand(playerHand, playerNodes, true, tablePane.getHeight() - 320);
    }

    private void layoutHand(List<Card> hand, List<Node> nodes, boolean isPlayer, double y){
        int count = hand.size();
        if(count == 0) return;

        double W = tablePane.getWidth();
        double margin = 40;
        double usable = W - 2 * margin;

        double spacing = (count <= 1) ? 0 : Math.min(160, Math.max(60, (usable - CARD_W)/(count - 1)));

        if(count >= 6) spacing *= 0.9;

        double groupW = (count - 1) * spacing + CARD_W;
        double startX = (W - groupW) / 2.0;

        for(int i=0; i<nodes.size(); i++){
            Node n = nodes.get(i);
            if(!n.getProperties().containsKey("anim")){
                n.setLayoutX(startX + i * spacing);
                n.setLayoutY(y);
            }
        }
    }

    private void drawDeck(){
        tablePane.getChildren().removeIf(n -> "DECK".equals(n.getUserData()));

        for(int i=0; i<3; i++){
            Rectangle back = new Rectangle(CARD_W, CARD_H);
            back.setArcWidth(18);
            back.setArcHeight(18);
            back.setFill(Color.web("#1E3278"));
            back.setStroke(Color.WHITE);
            back.setStrokeWidth(2);

            back.setLayoutX(deckX + i * 6);
            back.setLayoutY(deckY + i * 4);
            back.setEffect(new DropShadow(8, Color.color(0,0,0,0.35)));

            back.setUserData("DECK");
            tablePane.getChildren().add(back);
        }
    }

    // ===============================================================================================
    // UTILS
    // ===============================================================================================

    private void alert(String msg){
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private boolean confirm(String msg){
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        a.setHeaderText(null);
        Optional<ButtonType> r = a.showAndWait();
        return r.isPresent() && r.get() == ButtonType.YES;
    }

    private void setButtonsEnabled(boolean enabled){
        hitBtn.setDisable(!enabled);
        standBtn.setDisable(!enabled);
        doubleBtn.setDisable(!enabled);
        playAgainBtn.setDisable(enabled);
    }

    // ===============================================================================================
    // MAIN
    // ===============================================================================================

    public static void main(String[] args){
        launch(args);
    }
}
