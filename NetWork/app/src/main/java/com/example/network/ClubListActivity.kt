package com.example.network

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.network.adapters.ClubAdapter
import com.example.network.database.DatabaseRepository

class ClubListActivity : AppCompatActivity() {

    private lateinit var repository: DatabaseRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_club_list)

        repository = DatabaseRepository(this)

        val recyclerView = findViewById<RecyclerView>(R.id.clubRecycler)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val clubs = repository.getAllClubs()  // you already built this

        val adapter = ClubAdapter(clubs) { club ->
            val intent = Intent(this, ClubDetailsActivity::class.java)
            intent.putExtra("club_id", club.clubId)
            startActivity(intent)
        }

        recyclerView.adapter = adapter

        findViewById<Button>(R.id.createClubBtn).setOnClickListener {
            startActivity(Intent(this, CreateClubActivity::class.java))
        }

        findViewById<Button>(R.id.backBtn).setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()

        // refresh the list after creating a club
        val recyclerView = findViewById<RecyclerView>(R.id.clubRecycler)
        val clubs = repository.getAllClubs()
        recyclerView.adapter = ClubAdapter(clubs) { club ->
            val intent = Intent(this, ClubDetailsActivity::class.java)
            intent.putExtra("club_id", club.clubId)
            startActivity(intent)
        }
    }
}
