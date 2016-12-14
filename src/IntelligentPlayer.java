import java.util.ArrayList;
import java.util.Random;

public class IntelligentPlayer extends Player {
	Random rand = new Random();

	public IntelligentPlayer(int ID) {
		super(ID);

	}

	public Card playCard(State currentState) {
		return playCard(currentState, this.hand);
	}

	public Card playCardNaive(State currentState, ArrayList<Card>[] recursiveHand) {
		Card choice = null;
		int leadingSuit = currentState.getLeadingSuit();

		if (currentState.twoOfClubs == false) {
			// first move, play two of clubs
			currentState.twoOfClubs = true;
			choice = new Card(2, 2);
		} else if (leadingSuit == -1) {
			// if we are leading
			if (hasOnlyHearts()) {
				// only hearts - lowest heart
				choice = recursiveHand[0].get(0);
			} else {
				// lowest non heart
				choice = playLowestNonHeart();
			}
		} else if (isVoidCopy(leadingSuit, recursiveHand)) {
			if (hasQueen()) {
				// if queen drop queen
				choice = new Card(12, 3);
			} else if (recursiveHand[0].size() > 0) {
				// else if hearts drop hearts
				choice = recursiveHand[0].get(recursiveHand[0].size() - 1);
			} else {
				// else high card
				choice = playHighestNonHeartCard();
			}
		} else {
			// play lowest of suite
			choice = recursiveHand[leadingSuit].get(0);
		}
		this.removeFromCopy(choice, recursiveHand);
		return choice;
	}

	public Card playCard(State currentState, ArrayList<Card>[] recursiveHand) {

		// System.out.println("Now in Intelligent Player playCard");
		// currentState.printState();

		// this is baseline worst score to compare against card options
		int bestScore = 26*13;
		// this will be the card we output
		Card choice = null;

		int leadingSuit = currentState.getLeadingSuit();

		// first - copy hand into new arraylist
		ArrayList<Card>[] currentHand = copyHand(recursiveHand);

		// second - loop through valid options in your hand and play out game
		// if first round:
		if (currentState.twoOfClubs == false) {
			// first move, play two of clubs
			currentState.twoOfClubs = true;
			choice = new Card(2, 2);
		} else if (leadingSuit == -1) {
			if (currentState.heartsBroken) {
				// if you're leading
				// if hearts broken
				for (int suit = 0; suit < 4; suit++) {
					for (Card valid : currentHand[suit]) {
						int score = playout(valid, currentState, currentHand);
						System.out.println("Printing score from playCard: " + score + " with card " + valid.toString());
						if (score < bestScore) {
							System.out.println("Updating score to be  " + score+ " with card " + valid.toString());
							choice = valid;
							bestScore = score;
						}
					}
				}
			} else {
				// if you're leading
				// if not hearts broken
				// run through all valid cards - consider 1-3
				for (int suit = 1; suit < 4; suit++) {
					for (Card valid : currentHand[suit]) {
						int score = playout(valid, currentState, currentHand);
						System.out.println("Printing score from playCard: " + score + " with card " + valid.toString());
						if (score < bestScore) {
							System.out.println("Updating choice with " + score+ " with card " + valid.toString());
							choice = valid;
							bestScore = score;

						}
					}
				}
			}

		} else if (isVoidCopy(leadingSuit, currentHand)) {
			// we are void in suit, play any suit
			for (int suit = 0; suit < 4; suit++) {
				for (Card valid : currentHand[suit]) {
					int score = playout(valid, currentState, currentHand);
					System.out.println("Printing score from playCard: " + score + " with card " + valid.toString());
					if (score < bestScore) {
						System.out.println("Updating choice with " + score+ " with card " + valid.toString());
						choice = valid;
						bestScore = score;

					}
				}
			}
		} else {
			// not leading and not void - following in suit
			// run through all valid cards - consider leading suit
			for (Card valid : currentHand[leadingSuit]) {
				int score = playout(valid, currentState, currentHand);
				if (score < bestScore) {
					choice = valid;
					bestScore = score;

				}
			}

		}
		if (choice == null && hasOnlyHearts()) {
			int randCard = rand.nextInt(hand[0].size());
			choice = hand[0].get(randCard);
		}
		// remove from actual hand
		remove(choice);
		return choice;
	}

	private void setHand(ArrayList<Card>[] currentHand) {
		for (int suit = 0; suit < 4; suit++) {
			hand[suit].clear();
			for (Card card : currentHand[suit]) {
				hand[suit].add(card);
			}
		}

	}

	private int playout(Card choice, State prevState, ArrayList<Card>[] prevHand) {
//		System.out.println("Currently playing out: " + choice.toString());

		// keep track of points
		int points = 0;

		// make a safe copy of state
		State currentState = new State(prevState);

		// update copy
		// current player is our intelligent player
		currentState.updateState(choice, this.playerID);

		// update hand
		ArrayList<Card>[] currentHand = copyHand(prevHand);
		removeFromCopy(choice, currentHand);

		int counter = 1;// to keep track of correct player iteration
		// finish trick
		for (int i = currentState.cardsInTrick.size(); i < 4; i++) {
//			System.out.println("finish trick with " + currentState.deck.notPlayed.size() + " cards unplayed");

			Card opponentChoice = playRandomCard(currentState.deck, currentHand);
			int oppenentID = (this.playerID + counter) % 4;
			counter++;
			// System.out.println("finish trick simulating move by player " +
			// oppenentID);

			currentState.updateState(opponentChoice, oppenentID);
		}

		// find player to start next round
		int startingPlayer = currentState.winningPlayer();

		// check if we won points
		if (startingPlayer == this.playerID)
			points += currentState.points;// we won, add points to tally

		// continue playing out round
		int roundsRemaining = currentState.deck.played.size() / 4 + 1;
		// int trickNum = currentState.deck.played.size() / 4 + 1;

		for (int trick = roundsRemaining; trick < 14; trick++) {
//			System.out.println("Starting trick " + trick);

			// create new state
			ArrayList<Card> cardsInTrick = new ArrayList<Card>();
			State newState = new State(currentState.deck, currentState.heartsBroken, currentState.twoOfClubs,
					cardsInTrick, startingPlayer);

			// go through each of the 4 players and have them play
			for (int player = startingPlayer; player < startingPlayer + 4; player++) {
				Card newChoice = null;
				int currentPlayer = player % 4;

				// check if the current player is the Intelligent Player
				if (currentPlayer == this.playerID) {
					// recursively choose card
					newChoice = playCardNaive(newState, currentHand);

				} else {
					// simulate an opponents move
					newChoice = playRandomCard(newState.deck, currentHand);
				}

				// updates deck etc
				newState.updateState(newChoice, currentPlayer);

			}

			// trick is completed
			// set starting player to be the player who just won the round
			startingPlayer = newState.winningPlayer();

			// check if we won points
			if (startingPlayer == this.playerID)
				points += newState.points;// we won, add points to tally
		}
		roundsRemaining++;

//		System.out.println("POINTS: " + points);

		return points;
	}

	private Card playRandomCard(Deck currentDeck, ArrayList<Card>[] currentHand) {
		/*
		 * Method to return a random unplayed card that is not in the
		 * IntelligentPlayer's hand
		 */
		boolean valid = false;
		Card choice = null;
		while (!valid) {
			int index = rand.nextInt(currentDeck.notPlayed.size());
			choice = currentDeck.notPlayed.get(index);

			if (!inHand(choice, currentHand)) {
				// not in Intelligent Player's hand
				valid = true;
			}

		}
		return choice;
	}

	private boolean roundsRemaining(Deck deck) {
		/*
		 * if there are unplayed cards,there are rounds remaining
		 */
//		System.out.println("cards remaining: " + deck.notPlayed.size());
//		System.out.println(deck.notPlayed.size() > 0);
		return deck.notPlayed.size() > 0;
	}

	public ArrayList<Card>[] copyHand(ArrayList<Card>[] hand) {
		ArrayList<Card>[] currentHand = new ArrayList[4];
		for (int i = 0; i < 4; i++) {
			// copyHand[i].clear();
			currentHand[i] = new ArrayList<Card>();
			for (Card current : hand[i]) {
				// System.out.print(current.toString()+", ");
				currentHand[i].add(current);
			}
			// System.out.println("next suit");
		}
		return currentHand;
	}

	public void removeFromCopy(Card card, ArrayList<Card>[] currentHand) {
		// remove card from hand
		printCopyHand(currentHand);
		for (Card current : currentHand[card.suit]) {
			if (current.rank == card.rank) {
				currentHand[card.suit].remove(current);
				break;
			}
		}
	}

	public boolean inHand(Card card, ArrayList<Card>[] currentHand) {
		for (int i = 0; i < 4; i++) {
			for (Card mine : currentHand[i]) {
				if (mine.equals(card)) {
					return true;
				}
			}
		}
		return false;

	}

	private boolean isVoidCopy(int leadingSuit, ArrayList<Card>[] currentHand) {
		return currentHand[leadingSuit].size() == 0;
	}

	public void printCopyHand(ArrayList<Card>[] currentHand) {
		// for debugging
		int length = 0;
		for (ArrayList<Card> suit : currentHand) {
			// for (Object card : suit) {
			// System.out.println(card.toString());
			// }
			length += suit.size();
		}
	}

}
