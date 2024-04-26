package com.willeypianotuning.toneanalyzer.ui.splash.start

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.willeypianotuning.toneanalyzer.AppSettings
import com.willeypianotuning.toneanalyzer.R
import com.willeypianotuning.toneanalyzer.audio.PitchRaiseOptions
import com.willeypianotuning.toneanalyzer.databinding.FragmentStartWorkBinding
import com.willeypianotuning.toneanalyzer.extensions.runWithAudioPermission
import com.willeypianotuning.toneanalyzer.extensions.setDebounceOnClickListener
import com.willeypianotuning.toneanalyzer.store.db.tunings.PianoTuning
import com.willeypianotuning.toneanalyzer.ui.files.FilesActivity
import com.willeypianotuning.toneanalyzer.ui.main.MainActivity
import com.willeypianotuning.toneanalyzer.ui.pitch_raise.PitchRaiseConfigManager
import com.willeypianotuning.toneanalyzer.ui.tuning.TuningSettingsActivity
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class StartWorkFragment : Fragment(), PitchRaiseConfigManager.OnPitchRaiseConfigReadyListener {
    private var _binding: FragmentStartWorkBinding? = null
    private val binding get() = requireNotNull(_binding)

    @Inject
    lateinit var appSettings: AppSettings

    private val viewModel: StartWorkViewModel by viewModels()

    @Inject
    lateinit var pitchRaiseConfigManager: PitchRaiseConfigManager

    private val actionOpenTuningFile = registerForActivityResult(
        FilesActivity.Contract(),
        ::onOpenTuningFileResultReceived
    )

    private val actionConfigureNewTuning = registerForActivityResult(
        TuningSettingsActivity.Contract(),
        ::onConfigureTuningResultReceived
    )

    private val actionConfigureNewTuningForPitchRaise = registerForActivityResult(
        TuningSettingsActivity.Contract(),
        ::onConfigureTuningForPitchRaiseResultReceived
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_start_work, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStartWorkBinding.bind(view)

        binding.actionNewTuning.setDebounceOnClickListener {
            runWithAudioPermission {
                actionConfigureNewTuning.launch(viewModel.createNewTuning())
            }
        }
        binding.actionOpenTuning.setDebounceOnClickListener {
            if (!viewModel.isPro) {
                return@setDebounceOnClickListener
            }
            runWithAudioPermission {
                actionOpenTuningFile.launch(null)
            }
        }
        binding.actionResumeTuning.setDebounceOnClickListener {
            runWithAudioPermission {
                val state = viewModel.startWorkState.value as? StartWorkScreenState.Success
                    ?: return@runWithAudioPermission
                val tuning = state.lastTuning ?: return@runWithAudioPermission
                viewModel.tunePianoWith(tuning, null)
            }
        }
        binding.actionPitchRaise.setDebounceOnClickListener {
            if (!viewModel.isPro) {
                return@setDebounceOnClickListener
            }
            runWithAudioPermission {
                actionConfigureNewTuningForPitchRaise.launch(viewModel.createNewTuning())
            }
        }

        viewModel.startWorkState.observe(viewLifecycleOwner, this::onStartWorkStateChanged)
        viewModel.resumeWorkState.observe(viewLifecycleOwner, this::onResumeWorkStateChanged)
    }

    private fun onStartWorkStateChanged(state: StartWorkScreenState) {
        if (state is StartWorkScreenState.Loading) {
            binding.actionsLayout.visibility = View.GONE
            binding.loadingLayout.visibility = View.VISIBLE
            return
        }

        if (state is StartWorkScreenState.Success) {
            binding.actionResumeTuning.visibility =
                if (state.lastTuning != null) View.VISIBLE else View.GONE
            state.lastTuning?.let { tuning ->
                val tuningName = arrayOf(
                    tuning.make,
                    tuning.model,
                    tuning.name
                ).filter { it.isNotBlank() }.joinToString(" ")
                if (tuningName.isEmpty()) {
                    return@let
                }
                binding.textLastTuningName.text = tuningName
                binding.textLastTuningName.visibility = View.VISIBLE
            }
            binding.actionOpenTuning.visibility = if (state.isPro) View.VISIBLE else View.GONE
            binding.actionPitchRaise.visibility = if (state.isPro) View.VISIBLE else View.GONE
            binding.actionsLayout.visibility = View.VISIBLE
            binding.loadingLayout.visibility = View.GONE
        }
    }

    private fun onResumeWorkStateChanged(state: ResumeWorkState) {
        if (state is ResumeWorkState.NotSet) {
            binding.actionsLayout.visibility = View.VISIBLE
            binding.loadingLayout.visibility = View.GONE
            return
        }

        if (state is ResumeWorkState.Loading) {
            binding.actionsLayout.visibility = View.INVISIBLE
            binding.loadingLayout.visibility = View.VISIBLE
            return
        }

        if (state is ResumeWorkState.Success) {
            openTuningScreen()
        }
    }

    override fun onStart() {
        super.onStart()
        pitchRaiseConfigManager.onPitchRaiseConfigReadyListener = this
    }

    override fun onStop() {
        super.onStop()
        pitchRaiseConfigManager.onPitchRaiseConfigReadyListener = null
    }

    private fun openTuningScreen() {
        requireActivity().runWithAudioPermission {
            startActivity(Intent(requireActivity(), MainActivity::class.java))
            requireActivity().finish()
        }
    }

    private fun onOpenTuningFileResultReceived(tuningId: String?) {
        if (tuningId == null) {
            return
        }

        viewModel.loadTuning(tuningId)
    }

    private fun onConfigureTuningResultReceived(tuning: PianoTuning?) {
        if (tuning == null) {
            return
        }

        viewModel.onPianoTuningSelected(tuning)
        viewModel.tunePianoWith(tuning, null)
    }

    private fun onConfigureTuningForPitchRaiseResultReceived(tuning: PianoTuning?) {
        if (tuning == null) {
            return
        }

        Timber.d("Tuning for raise config has been configured")
        viewModel.onPianoTuningSelected(tuning)
        pitchRaiseConfigManager.startNewConfiguration(requireActivity(), tuning)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onPitchRaiseConfigReady(config: PitchRaiseOptions) {
        Timber.d("Pitch raise config is ready")
        val state = viewModel.selectedTuning.value as? StartWorkState.TuningSelected ?: kotlin.run {
            Timber.e("No tuning found for pitch raise")
            return
        }
        viewModel.tunePianoWith(state.tuning, config)
    }

    override fun onPitchRaiseCancelled() {
        Timber.d("Pitch raise was cancelled")
        viewModel.removeSelectedTuning()
    }
}