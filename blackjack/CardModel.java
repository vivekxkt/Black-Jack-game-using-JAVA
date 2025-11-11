package blackjack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class CardModel {

    // ===== Suits =====
    public enum Suit {
        SPADES("♠"), HEARTS("♥"), DIAMONDS("♦"), CLUBS("♣");

        public final String symbol;
        Suit(String s){ this.symbol = s; }

        public boolean isRed(){
            return this == HEARTS || this == DIAMONDS;
        }
    }

    // ===== Ranks =====
    public enum Rank {
        ACE("A",1),
        TWO("2",2), THREE("3",3), FOUR("4",4), FIVE("5",5),
        SIX("6",6), SEVEN("7",7), EIGHT("8",8), NINE("9",9),
        JACK("J",10), QUEEN("Q",10), KING("K",10);

        public final String label;
        public final int numeric;

        Rank(String l, int n){
            this.label = l;
            this.numeric = n;
        }
    }

    // ===== Card Object =====
    public static class Card {
        public final Suit suit;
        public final Rank rank;

        public Card(Suit s, Rank r){
            this.suit = s;
            this.rank = r;
        }
    }

    // ===== Deck =====
    public static class Deck {
        private final List<Card> cards = new ArrayList<>();
        private final Random random = new Random();

        public Deck(){
            for(Suit s : Suit.values()){
                for(Rank r : Rank.values()){
                    cards.add(new Card(s, r));
                }
            }
            shuffle();
        }

        public void shuffle(){
            Collections.shuffle(cards, random);
        }

        public Card draw(){
            return cards.remove(cards.size() - 1);
        }

        public int size(){
            return cards.size();
        }
    }
}
