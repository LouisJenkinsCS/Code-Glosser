#include "PerfectGame.h"

namespace ns_game
{
    void PerfectGame::process_turn(ns_matrix::Matrix<int>& board, 
            const enum Player &turn)
    {
        turn == AI ? ai_turn(board) : player_turn(board);
    }
    
    void PerfectGame::ai_turn(ns_matrix::Matrix<int>& board)
    {
        int best_score = -11;
        int best_position = -1;

        /*
         * First, we must check each possible space on the board and
         * recursively check to see if it is the best possible move.
         */
        for(int i = 0; i < 9; i++)
        {
            int& position = board[i / 3][i % 3];

            // If position on the board belongs to no one...
            if(position == NONE)
            {
                position = AI;

                // Can we win with this move?
                if(has_player_won(board, AI)) 
                {
                    best_position = i;
                    break;
                }

                // If not, look ahead of the board for best move.
                int score = -predict_best_turn(board, PLAYER, 0);

                // As this is a simulation, reset board changes.
                position = NONE;

                // If this move is better than another move, consider it
                if(score > best_score)
                {
                    best_score = score;
                    best_position = i;             
                }
            }
        }

        // If the board is full
        if(best_position == -1)
            return;

        board[best_position / 3][best_position % 3] = AI;
    }
    
    int PerfectGame::predict_best_turn(ns_matrix::Matrix<int>& board,
        const enum Player& player, int depth)
    {
        int best_score = -11;
        int best_position = -1;

        for(int i = 0; i < 9; i++) 
        {
            int& position = board[i / 3][i % 3];

            // If position on the board belongs to no one...
            if(position == NONE)
            {
                position = player;

                // Does the current player win with this move?
                if(has_player_won(board, player)) 
                {
                    best_position = i;
                    best_score = player == AI ? depth - 10 : 10 - depth;
                    position = NONE;

                    break;
                }

                 // If not, look ahead of the board for best move.
                int score = -predict_best_turn(board,
                        player == AI ? PLAYER : AI, depth + 1);

                // As this is a simulation, reset board changes.
                position = NONE;

                // If this move is better than another move, consider it
                if(score > best_score)
                {
                    best_score = score;
                    best_position = i;
                }
            }
        }

        // If the board is full, it is a draw.
        if(best_position == -1)
            return 0;
        else
            return best_score;
    }
    
    void PerfectGame::player_turn(ns_matrix::Matrix<int>& board)
    {
        int x = -1, y = -1;

        while(true)
        {
            x = -1, y = -1;
            std::cout << "X,Y ∈ { 1, 2, 3 }" << std::endl 
                << "Next Move: { X, Y }" << std::endl << "=> ";

            std::string line;
            std::getline(std::cin, line, ',');

            std::stringstream stream(line);
            stream >> x;
            std::cin >> y;

            if((x < 1 || x > 3) || (y < 1 || y > 3)) 
            {
                std::cout << "X and Y must be between 1 and 3" 
                        << std::endl;

                continue;
            }

            if(board[x - 1][y - 1] != NONE)
            {
                std::cout << "Sorry but that position is already taken"
                        << std::endl;

                continue;
            }

            break;
        }

        board[x-1][y-1] = PLAYER;
    }
}