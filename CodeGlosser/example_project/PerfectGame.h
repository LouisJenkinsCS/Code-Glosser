/* 
 * File:   PerfectGame.h
 * Author: theif519
 *
 * The Perfect Game is a game between two players; a Human player and an AI. The
 * implementor of this must handle determining if either player has won the game
 * and printing out the actual board. The PerfectGame handles processing each
 * player turn.
 * 
 * The AI is implemented with a modified version of the minimax algorithm, which
 * originally this was supposed to follow precisely, except due to a certain
 * bug in the algorithm preventing the AI from winning the game, merely always
 * resulting in a draw. Hence, it has been modified to be more greedy, and hence
 * vulnerable to losing. If it is possible, within the next move, to win the
 * game, it will attempt to do so, while also resulting in negligence in the
 * long-term predictions, allowing the Human player to win the game.
 */

#ifndef PERFECTGAME_H
#define	PERFECTGAME_H

#include "Game.h"
#include <iostream>
#include <sstream>

namespace ns_game 
{
    class PerfectGame : public Game 
    {
        public:
            PerfectGame() :
            Game() { }
            
            /**
             * Processes the turn for either the AI or PLAYER depending on whose
             * turn it is.
             * @param board Game Board
             * @param turn Turn
             */
            void process_turn(ns_matrix::Matrix<int>& board,
                    const enum Player &turn);
            
        private:
            
            /**
             * The AI will attempt to predict the BEST possible move that will
             * lead to it's success, and to do so it requires quite a bit of
             * recursion. The first thing the AI does is, for each spot on the
             * board, if it is open, predict all possible paths, and if it will
             * result in a loss, we go to the next one until we can finally find
             * one.
             * @param board Game board.
             */
            void ai_turn(ns_matrix::Matrix<int>& board);
            
            /**
             * Uses the minimax algorithm to determine the perfect move to take.
             * This is called for the AI's turn, and used to simulate the next
             * most viable moves for each player. 
             * 
             * The minimax algorithm rates moves based on scores, from -10 to 10
             * where -10 represents PLAYER win, 10 represents AI win, and 0
             * represents a draw. Scoring is also affected by the current depth,
             * or simulated moves, to force the AI to make sensible moves to
             * draw out the game as long as possible when it is sure to lose.
             * 
             * It should note that given the perfect best move each time, it is
             * impossible to defeat the AI, and at best for the player it will
             * end in a draw.
             * @param board Game board.
             * @param player Which player's turn do we simulate?
             * @param depth Current simulated number of turns.
             * @return The best score for the player given the board.
             */
            int predict_best_turn(ns_matrix::Matrix<int>& board,
                const enum Player& player, int depth);
            
            /**
             * Asks the player for input on the next move. The move must have
             * both X and Y coordinates be between 1 and 3, and also for the
             * chosen move position to not be occupied.
             * @param board Game board.
             */
            void player_turn(ns_matrix::Matrix<int>& board);
    };
}

#endif	/* PERFECTGAME_H */

