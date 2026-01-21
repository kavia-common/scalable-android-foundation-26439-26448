package org.example.app.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import org.example.app.R
import org.example.app.data.InMemoryAppRepository
import org.example.app.ui.common.RepositoryViewModelFactory

class DashboardFragment : Fragment() {

    private val viewModel: DashboardViewModel by viewModels {
        RepositoryViewModelFactory(
            repository = InMemoryAppRepository(),
            screenKey = "dashboard"
        ) { repo, key ->
            DashboardViewModel(repo, key)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_dashboard, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val title = view.findViewById<TextView>(R.id.title)
        val counter = view.findViewById<TextView>(R.id.counter_value)
        val increment = view.findViewById<Button>(R.id.increment_button)

        viewModel.title.observe(viewLifecycleOwner) { title.text = it }
        viewModel.counter.observe(viewLifecycleOwner) { counter.text = it.toString() }

        increment.setOnClickListener { viewModel.increment() }
    }
}
