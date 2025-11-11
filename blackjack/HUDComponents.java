package blackjack;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class HUDComponents {

    // Better, darker, cleaner glass panel
    public static VBox glassCard(String title){
        VBox box = new VBox(12);
        box.setPadding(new Insets(16));
        box.setAlignment(Pos.TOP_LEFT);
        box.setMinWidth(260);

        // Darkened glass (more readable)
        box.setBackground(new Background(
                new BackgroundFill(
                        Color.rgb(10, 15, 18, 0.75),
                        new CornerRadii(18),
                        Insets.EMPTY
                )
        ));

        // Softer border
        box.setBorder(new Border(
                new BorderStroke(
                        Color.rgb(255,255,255,0.10),
                        BorderStrokeStyle.SOLID,
                        new CornerRadii(18),
                        new BorderWidths(1)
                )
        ));

        // Clean shadow
        box.setEffect(new DropShadow(20, Color.color(0,0,0,0.55)));

        // Title styling
        Label t = new Label(title);
        t.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        t.setTextFill(Color.WHITE);

        box.getChildren().add(t);
        return box;
    }


    // Cleaner graph canvas
    public static Canvas makeGraphCanvas(){
        Canvas c = new Canvas(260, 90);
        c.setStyle("-fx-background-color: transparent;");
        return c;
    }

    // Clean stat text line
    public static Label statLabel(String text){
        Label l = new Label(text);
        l.setFont(Font.font("Segoe UI", 15));
        l.setTextFill(Color.web("#E8EEF2"));
        return l;
    }

    // Slightly bigger stat headers
    public static Label statHeader(String text){
        Label l = new Label(text);
        l.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 17));
        l.setTextFill(Color.WHITE);
        return l;
    }

    // Session / Totals values
    public static Label hudValue(String text){
        Label l = new Label(text);
        l.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 17));
        l.setTextFill(Color.web("#F4F6F7"));
        return l;
    }
}
