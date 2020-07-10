package com.example.dynamite

import com.softwire.dynamite.bot.Bot
import com.softwire.dynamite.game.Gamestate
import com.softwire.dynamite.game.Move

class MyBot : Bot {
    override fun makeMove(gamestate: Gamestate): Move {
        // Are you debugging?
        // Put a breakpoint in this method to see when we make a move

        var dyCount = 0
        var oppDyCount = 0
        var dyAllowed = 1
        var oppDyAllowed = 1

        var matrix :  MutableMap<String, MutableList<nextMove>> = createMatrix()    // Create Markov Chain matrix

        if (gamestate.getRounds().count() > 0) {                // If not first move
            for (round in gamestate.rounds) {                   // Check numb dynamites
                if (round.getP1() == Move.D) {
                    dyCount += 1
                }
                else if (round.getP2() == Move.D) {
                    oppDyCount += 1
                }
            }
            if (dyCount == 100){
                dyAllowed = 0
            }
            else if (oppDyCount == 100){
                oppDyAllowed = 0
            }

            matrix = updateNOcc(gamestate, matrix)                  // Update number occurrences of every possible move from each situation
            matrix = updateProbs(gamestate, matrix, oppDyAllowed)   // Update probability of every possible move from each situation

            val oppMove = getOppMove(gamestate, matrix)             // Predict opponents move
            val moveToPlay = oppMove?.let { playMove(it, dyAllowed, gamestate, matrix) }    // Select move to counter opponents
            if (moveToPlay != null){
                return moveToPlay
            }
        }
        else{
            return Move.D
        }
        return Move.S
    }

    fun createMatrix(): MutableMap<String, MutableList<nextMove>> {

        var keys = listOf("R", "P", "S", "D", "W")
        var keyMoves = listOf(Move.R, Move.P, Move.S, Move.D, Move.W)
        var fullKeys = mutableListOf<String>()
        var matrix = mutableMapOf<String, MutableList<nextMove>>()

        for (key in keys){
            for (otherKey in keys){
                fullKeys.add(key.plus(otherKey))
            }
        }

        for (fullKey in fullKeys) {
            var newMoves = mutableListOf<nextMove>()
            for (key in keyMoves){
                var newMove : nextMove = nextMove(key, 0.2, 0)
                newMoves.add(newMove)
            }
            matrix[fullKey] = newMoves
        }
        return matrix
    }

    fun updateNOcc(gamestate: Gamestate, matrix: MutableMap<String, MutableList<nextMove>>): MutableMap<String, MutableList<nextMove>> {
        val numbRounds : Int = gamestate.getRounds().count()
        var index : Int = 0
        while (index < numbRounds-1){
            val state : String = (gamestate.getRounds()[index].getP1()).toString() + (gamestate.getRounds()[index].getP2()).toString()
            val next : Move = (gamestate.getRounds()[index+1].getP2())

            for (possMove in matrix[state]!!){
                if (possMove.name == next){
                    possMove.nOcc += 1
                }
            }
            index ++
        }
        return matrix
    }

    fun updateProbs(gamestate: Gamestate, matrix: MutableMap<String, MutableList<nextMove>>, oppDyAllowed:Int): MutableMap<String, MutableList<nextMove>>{
        for (key in matrix.keys){
            var totalOcc : Int = 0
            for (possMove in matrix[key]!!) {
                totalOcc += possMove.nOcc
            }
            for (possMove in matrix[key]!!) {
                if (possMove.name == Move.D && oppDyAllowed == 0){
                    possMove.prob = 0.0
                }
                else {
                    possMove.prob = (possMove.nOcc).toDouble() / totalOcc.toDouble()
                }
            }
                //println(key + matrix[key])
        }
        //println()
        return matrix
    }

    fun getOppMove(gamestate: Gamestate, matrix: MutableMap<String, MutableList<nextMove>>): Move? {
        val prevState : String = (gamestate.getRounds()[gamestate.getRounds().count()-1].getP1()).toString() + (gamestate.getRounds()[gamestate.getRounds().count()-1].getP2()).toString()

        val rand2 = (1..3).shuffled().first()

        var move : Move? = null
        var move2: Move? = null
        var totalProb = 0.0
        var rand = Math.random()
        var set = 0

        for (possMove in matrix[prevState]!!){
            totalProb += possMove.prob
            if (totalProb > rand && set == 0){
                move = possMove.name
                set = 1
            }
        }

        val myPrevState : Move = gamestate.getRounds()[gamestate.getRounds().count()-1].getP1()

        if (move == null){
            for (key in matrix.keys){
                if (key[0].toString() == myPrevState.toString() && set == 0){
                    totalProb = 0.0
                    for (possMove in matrix[key]!!) {
                        totalProb += possMove.prob
                        if (totalProb > rand && set == 0) {
                            move = possMove.name
                            set = 1
                        }
                    }

                }
            }
        }
        if (move == null){
            if (rand2 == 1){
                move = Move.P
            }
            else if (rand2 == 2) {
                move = Move.R
            }
            else{
                move = Move.S
            }
        }

        //println(probDict)
        return move
    }

    fun playMove(oppMove: Move,dyAllowed:Int,gamestate: Gamestate, matrix: MutableMap<String, MutableList<nextMove>>): Move? {

        val beats = mutableMapOf<Move, Move>()
        var _oppMove = oppMove

        var waterMov : Move = Move.S
        var dyMov : Move = Move.S
        var rockMov : Move = Move.P
        var paperMov : Move = Move.S
        var sciMov : Move = Move.R

        var rand1 = (1..3).shuffled().first()
        var rand2 = (1..50).shuffled().first()
        var rand3 = (1..50).shuffled().first()
        var rand4 = (1..50).shuffled().first()
        var rand5 = (1..3).shuffled().first()

        var dyProb = 50
        var set = 0
        var totalProb = 0.0
        var newMove : Move? = null
        val prevState : String = (gamestate.getRounds()[gamestate.getRounds().count()-1].getP1()).toString() + (gamestate.getRounds()[gamestate.getRounds().count()-1].getP2()).toString()


        if (rand1 == 1){
            waterMov = Move.R
        }
        else if (rand1 ==2){
            waterMov = Move.P
        }

        if (dyAllowed == 1) {
            if (rand2 > dyProb) {
                rockMov = Move.D
            }

            if (rand3 > dyProb) {
                paperMov = Move.D
            }

            if (rand4 > dyProb) {
                sciMov = Move.D
            }
            dyMov = Move.D
        }
        else {
            if (oppMove == Move.D) {
            var rand = Math.random()
                for (possMove in matrix[prevState]!!) {
                    totalProb += possMove.prob
                    if (totalProb > rand && set == 0) {
                        newMove = possMove.name
                        set = 1
                    }
                }
                if (newMove != null){
                    _oppMove = newMove
                }
            }
            if (rand5 == 1) {
                dyMov = Move.R
            } else if (rand1 == 2) {
                dyMov = Move.P
            }
        }

        beats[Move.S] = sciMov
        beats[Move.R] = rockMov
        beats[Move.P] = paperMov
        beats[Move.D] = dyMov
        beats[Move.W] = waterMov

        return beats[_oppMove]
    }

    init {
        // Are you debugging?
        // Put a breakpoint on the line below to see when we start a new match
        println("Started new match")
    }

    data class nextMove(var name: Move, var prob: Double, var nOcc : Int)

}
