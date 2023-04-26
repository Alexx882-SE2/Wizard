package at.aau.edu.wizards.ui.discover

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import at.aau.edu.wizards.MainActivity
import at.aau.edu.wizards.api.Client
import at.aau.edu.wizards.databinding.FragmentDiscoverBinding
import at.aau.edu.wizards.ui.discover.recycler.DiscoverAdapter

class DiscoverFragment : Fragment() {

    private val viewModel by lazy {
        ViewModelProvider(
            this,
            DiscoverViewModel.Factory(Client.getInstance(requireContext()))
        )[DiscoverViewModel::class.java]
    }

    private var binding: FragmentDiscoverBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentDiscoverBinding.inflate(inflater, container, false)

        this.binding = binding

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
    }

    override fun onDestroyView() {
        viewModel.stopDiscovery()
        binding = null
        super.onDestroyView()
    }

    private fun setupUI() {
        val binding = this.binding ?: return
        val adapter = DiscoverAdapter { clickedPending ->
            viewModel.connectEndpoint(clickedPending)
        }

        binding.discoverRecycler.adapter = adapter

        viewModel.items.observe(viewLifecycleOwner) { endpoints ->
            adapter.submitList(endpoints)
        }

        viewModel.startGame.observe(viewLifecycleOwner) {
            val mainActivity = activity as? MainActivity ?: return@observe

            mainActivity.showGame(asClient = true)
        }

        viewModel.startDiscovery()
    }
}