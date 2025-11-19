package com.example.network

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.network.adapters.ClubAdapter
import com.example.network.database.DatabaseRepository
import android.text.Editable
import android.text.TextWatcher
import com.google.android.material.textfield.TextInputEditText
import com.example.network.model.Club

class ClubListActivity : AppCompatActivity() {

    private lateinit var repository: DatabaseRepository
    private lateinit var clubAdapter: ClubAdapter
    private lateinit var clubRecycler: RecyclerView
    private lateinit var searchField: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_club_list)

        repository = DatabaseRepository(this)

        clubRecycler = findViewById<RecyclerView>(R.id.clubRecycler)
        clubRecycler.layoutManager = LinearLayoutManager(this)

        val clubs = repository.getAllClubs()

        val adapter = ClubAdapter(clubs) { club ->
            val intent = Intent(this, ClubDetailsActivity::class.java)
            intent.putExtra("club_id", club.clubId)
            startActivity(intent)
        }

        val initialClubs = repository.getAllClubs()
        clubAdapter = ClubAdapter(initialClubs) { club ->
            val intent = Intent(this, ClubDetailsActivity::class.java)
            intent.putExtra("club_id", club.clubId)
            startActivity(intent)
        }
        clubRecycler.adapter = clubAdapter

        // 2. Setup Search Functionality
        searchField = findViewById(R.id.searchClubField) as TextInputEditText
        searchField.addTextChangedListener(searchTextWatcher)

        findViewById<Button>(R.id.createClubBtn).setOnClickListener {
            startActivity(Intent(this, CreateClubActivity::class.java))
        }

        findViewById<Button>(R.id.backBtn).setOnClickListener {
            finish()
        }
    }

    private fun loadClubs(query: String) {
        Thread {
            val clubs: List<Club> = if (query.isBlank()) {
                repository.getAllClubs()
            } else {
                repository.searchClubs(query)
            }

            runOnUiThread {
                // Update the adapter's data and notify the RecyclerView
                clubAdapter.updateData(clubs)
            }
        }.start()
    }

    private val searchTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
            val query = s.toString()
            if (query.isBlank()) {
                loadClubs("") // Load all clubs if the search box is cleared
            } else {
                loadClubs(query) // Search with the query
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadClubs(searchField.text.toString())
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::repository.isInitialized) {
            repository.close()
        }
    }
}
