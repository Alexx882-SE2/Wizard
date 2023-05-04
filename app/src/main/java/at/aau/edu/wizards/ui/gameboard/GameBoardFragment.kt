package at.aau.edu.wizards.ui.gameboard


import android.os.Bundle
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnDragListener
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import at.aau.edu.wizards.R
import at.aau.edu.wizards.api.Client
import at.aau.edu.wizards.api.Server
import at.aau.edu.wizards.databinding.FragmentGameboardBinding
import at.aau.edu.wizards.databinding.ItemCardBinding
import at.aau.edu.wizards.ui.gameboard.claim.GuessAdapter
import at.aau.edu.wizards.ui.gameboard.recycler.GameBoardAdapter
import at.aau.edu.wizards.ui.gameboard.recycler.GameBoardBoardAdapter
import at.aau.edu.wizards.ui.gameboard.recycler.GameBoardHeaderAdapter
import at.aau.edu.wizards.util.OffsetItemDecoration
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class GameBoardFragment : Fragment(), OnDragListener {

    private val asClient by lazy {
        requireArguments().getBoolean(AS_CLIENT_EXTRA)
    }

    private val amountCpu by lazy {
        requireArguments().getInt(AMOUNT_CPU_EXTRA)
    }

    private var binding: FragmentGameboardBinding? = null

    private val viewModel by lazy {
        val factory = GameBoardViewModelFactory(
            asClient = asClient,
            amountCpu = amountCpu,
            server = Server.getInstance(requireContext()),
            client = Client.getInstance(requireContext())
        )
        ViewModelProvider(this, factory)[GameBoardViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val activity = requireActivity()

        activity.onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                MaterialAlertDialogBuilder(activity)
                    .setMessage(getString(R.string.leave_warning))
                    .setPositiveButton(getString(R.string.no), null)
                    .setNegativeButton(getString(R.string.yes)) { _, _ ->
                        isEnabled = false
                        activity.onBackPressedDispatcher.onBackPressed()
                    }
                    .create()
                    .show()
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentGameboardBinding.inflate(inflater, container, false)

        this.binding = binding

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun setupUI() {
        val binding = this.binding ?: return

        setupHand(binding)
        setupBoard(binding)
        setupGuess(binding)
        setupHeader(binding)

        viewModel.trump.observe(viewLifecycleOwner) { trump ->
            binding.trumpIndicatorCard.root.setImageResource(trump.image())
        }
    }

    private fun setupHand(binding: FragmentGameboardBinding) {
        val handAdapter = GameBoardAdapter {
            viewModel.sendMessage(it.getString())
        }

        binding.gameboardRecyclerView.adapter = handAdapter
        binding.gameboardRecyclerView.addItemDecoration(OffsetItemDecoration(90))

        viewModel.cards.observe(viewLifecycleOwner) { cards ->
            handAdapter.submitList(cards)
        }
    }

    private fun setupGuess(binding: FragmentGameboardBinding) {
        val guessAdapter = GuessAdapter { guess ->
            // should not access GameModel here
            viewModel.gameModel.sendGuessOfLocalPlayer(guess)
        }

        binding.guessRecycler.adapter = guessAdapter

        viewModel.guess.observe(viewLifecycleOwner) { guess ->
            guessAdapter.submitList(guess)
        }
    }

    private fun setupBoard(binding: FragmentGameboardBinding) {
        val boardAdapter = GameBoardBoardAdapter()

        binding.boardRecycler.adapter = boardAdapter
        binding.boardRecycler.addItemDecoration(OffsetItemDecoration(310))

        viewModel.board.observe(viewLifecycleOwner) { cards ->
            boardAdapter.submitList(cards)
        }
    }

    private fun setupHeader(binding: FragmentGameboardBinding) {
        val adapterHeader = GameBoardHeaderAdapter()

        binding.headerRecycler.adapter = adapterHeader

        viewModel.headersWithCurrentPlayer.observe(viewLifecycleOwner) {
            val headers = it.first
            val currentPlayer = it.second

            adapterHeader.submitList(headers) {
                binding.gameboardRecyclerView.smoothScrollToPosition(currentPlayer)
            }
        }

        binding.dragContainer.setOnDragListener(this)
    }

    override fun onDrag(view: View, event: DragEvent): Boolean {
        val binding = this.binding
        if (event.action == DragEvent.ACTION_DROP && binding != null) {

            val dropX = event.x
            val dropY = event.y
            val item = event.localState as Cards
            val inflater = LayoutInflater.from(requireContext())
            val shape = ItemCardBinding.inflate(inflater, binding.dragContainer,false)

            shape.root.x = dropX
            shape.root.y = dropY

            binding.dragContainer.addView(shape.root)


        }

        return true
    }

    companion object {
        private const val AS_CLIENT_EXTRA = "AS_CLIENT_EXTRA"
        private const val AMOUNT_CPU_EXTRA = "AMOUNT_CPU_EXTRA"
        fun instance(asClient: Boolean, amountCpu: Int = 0): GameBoardFragment {
            if (asClient && amountCpu > 0) {
                // This is not handled ideally, but fine for now
                throw IllegalArgumentException("Only Server is allowed to define cpu players")
            }

            return GameBoardFragment().apply {
                arguments = bundleOf(
                    AS_CLIENT_EXTRA to asClient,
                    AMOUNT_CPU_EXTRA to amountCpu
                )
            }
        }
    }

}

