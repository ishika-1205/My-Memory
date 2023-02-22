package com.ic.mymemory

import android.annotation.SuppressLint
import android.content.Intent
import android.icu.text.CaseMap.Title
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.vectordrawable.graphics.drawable.ArgbEvaluator
import com.google.android.material.snackbar.Snackbar
import com.ic.mymemory.models.*
import com.ic.mymemory.utils.EXTRA_BOARD_SIZE

class MainActivity : AppCompatActivity() {

    companion object{
        private const val  TAG = "MainActivity"
        private const val CREATE_REQUEST_CODE = 248
    }

    private lateinit var clRoot: ConstraintLayout
    private lateinit var adapter: MemoryBoardAdapter
    private lateinit var memoryGame: MemoryGame
    private lateinit var rvBoard: RecyclerView
    private lateinit var tvMoves: TextView
    private lateinit var tvPairs: TextView

    private var boardSize: BoardSize = BoardSize.EASY

    @SuppressLint("MissingInflatedId", "SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        clRoot = findViewById(R.id.clRoot)
        rvBoard = findViewById(R.id.rvBoard)
        tvMoves = findViewById(R.id.tvMoves)
        tvPairs = findViewById(R.id.tvPairs)


        //below 3 lines are just a good developer practise since while checking the create activity everytime we dont have to go through all those menu option navigations again and again
        //we are obviously not pushing this into production
        //improves efficiency of getting to create activity as our app gets a liitle more complicating

        val intent = Intent(this, CreateActivity::class.java)
        intent.putExtra(EXTRA_BOARD_SIZE, BoardSize.MEDIUM)
        startActivity(intent)

        setUpBoard()

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.miRefresh -> {
                if(memoryGame.getNumMoves() > 0 && !memoryGame.haveWonGame()){
                    showAlertDialog("Quit your current game?", null, View.OnClickListener {
                        setUpBoard()
                    })
                }else {
                    setUpBoard()
                }
                return true//set up the game again
            }
            R.id.mi_newSize -> {
                showNewSizeDialog()
                    return true
            }
            R.id.mi_custom -> {
                showCreationDialog()
                return true
            }

        }

return super.onOptionsItemSelected(item)
}

    private fun showCreationDialog() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size, null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.RadioGroup)

        showAlertDialog("Create your own memory board", boardSizeView, View.OnClickListener {
            val desiresBoardSize = when(radioGroupSize.checkedRadioButtonId){
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
            //navigate the user to a new activity
            val intent = Intent(this, CreateActivity:: class.java )
            intent.putExtra(EXTRA_BOARD_SIZE, desiresBoardSize)
            startActivityForResult(intent, CREATE_REQUEST_CODE)
        })
    }

    private fun showNewSizeDialog() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size, null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.RadioGroup)
        when(boardSize){
            BoardSize.EASY -> radioGroupSize.check(R.id.rbEasy)
            BoardSize.MEDIUM-> radioGroupSize.check(R.id.rbMedium)
            BoardSize.HARD -> radioGroupSize.check(R.id.rbHard)
        }
        showAlertDialog("Chose new size", boardSizeView, View.OnClickListener {
            boardSize = when(radioGroupSize.checkedRadioButtonId){
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
        setUpBoard()//set a new value for the board size
        })
    }

    private fun showAlertDialog(title: String, view: View?, positiveClickListener: View.OnClickListener) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(view)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Ok"){ _,_ ->
                positiveClickListener.onClick(null)
            }.show()

    }

    //2 important components of recycler view - (ADAPTER - takes the underlying data set of the recycler view and turning it or adapting each piece of data into a view  & LAYOUT MANAGER - given the views to be shown in recycler view it measures and positions those item views
    private fun setUpBoard(){
        when (boardSize){
            BoardSize.EASY -> {
                tvMoves.text = "Easy: 4*2"
                tvPairs.text = "pairs: 0/4"
            }
            BoardSize.MEDIUM -> {
                tvMoves.text = "Medium: 6*3"
                tvPairs.text = "pairs: 0/9"
            }
            BoardSize.HARD -> {
                tvMoves.text = "Hard: 6*6"
                tvPairs.text = "pairs: 0/12"
            }

        }
        memoryGame = MemoryGame(boardSize )
        adapter = MemoryBoardAdapter(this, boardSize, memoryGame.cards, object: MemoryBoardAdapter.CardClickListener{

            override fun onCardClicked(position: Int) {
                //  Log.i(TAG, "card clicked $position") We are deleting this log statement to delegate its work into a method called update Game with flip
                updateGameWithFlip(position)
            }
        })

        rvBoard.adapter = adapter
        rvBoard.setHasFixedSize(true) // a performance optimization to ensure that the size of recycler view is not affected by adapter components rvsize is always defined as soon as the app boots
        rvBoard.layoutManager = GridLayoutManager(this, boardSize.getWidth())
    }


    @SuppressLint("RestrictedApi")
    private fun updateGameWithFlip(position: Int) {
        //Error Handling for flipping over the cards
        //case1 - if we won the game
        if(memoryGame.haveWonGame()){

            Snackbar.make(clRoot, "you already won", Snackbar.LENGTH_SHORT).show()//to show some UI changes when any of these invalid moves is made
            return //alert the user about an invalid move
        }
        if(memoryGame.isCardFaceup(position)){

            Snackbar.make(clRoot, "invalid move", Snackbar.LENGTH_SHORT).show()
            return // alert the user about an invalid move
        }

        //here we are actually flipping over the cards
        if(memoryGame.flipCard(position)){
            Log.i(TAG, "Found a match! number of pairs found: ${memoryGame.numPairsFound}")
            // this color is a result of interpolation - i am walking 1000 steps and i say am 75% done u will assume i walked 750 steps means u are interpolating/estimating
            val color = ArgbEvaluator().evaluate(
                memoryGame.numPairsFound.toFloat()/boardSize.getNumPairs(),
                ContextCompat.getColor(this, R.color.color_progress_none),
                        ContextCompat.getColor(this, R.color.color_progress_full)
            ) as Int
            tvPairs.setTextColor(color)
            tvPairs.text = "Pairs: ${memoryGame.numPairsFound} / ${boardSize.getNumPairs()}"
            if(memoryGame.haveWonGame()){
                Snackbar.make(clRoot, "You won Congratulations", Snackbar.LENGTH_SHORT).show()
            }
        }

        tvMoves.text = "Moves: ${memoryGame.getNumMoves()}"
        adapter.notifyDataSetChanged()

    }
}