package com.sudoku.service;

import com.sudoku.dto.PerformanceAnalysis;
import com.sudoku.model.Game;
import com.sudoku.model.HintHistory;
import com.sudoku.model.Move;
import com.sudoku.model.PlayerPerformance;
import com.sudoku.repository.PlayerPerformanceRepository;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LearningAnalysisService {
    private static final String[] LABELS = {"Easy", "Medium", "Hard"};

    private final PlayerPerformanceRepository performanceRepository;

    public LearningAnalysisService(PlayerPerformanceRepository performanceRepository) {
        this.performanceRepository = performanceRepository;
    }

    public PerformanceAnalysis analyzeAndPersist(Game game, List<Move> moves, List<HintHistory> hints) {
        int attempts = moves.size() + game.getMistakes();
        int accuracy = attempts == 0 ? 100 : Math.max(0,
                (int) Math.round((moves.size() * 100.0) / attempts));
        String strongArea = hints.stream().anyMatch(h -> "NAKED_SINGLE".equals(h.getTechnique()))
                ? "Single-candidate deduction"
                : "Board scanning and rule awareness";
        String improvement = game.getMistakes() > 0
                ? "Check row, column, and box conflicts before placing a value"
                : hints.size() > Math.max(2, moves.size() / 3)
                ? "Try listing candidates before requesting another hint"
                : "Look for hidden singles in each row, column, and box";

        ModelPrediction prediction = predict(game, accuracy, moves.size(), hints.size());
        String recommendation = prediction.label().equals("Easy")
                ? "Practice Easy puzzles to improve accuracy"
                : prediction.label().equals("Medium")
                ? "Practice candidate elimination on Medium puzzles"
                : "Try harder puzzles and focus on advanced deductions";

        PerformanceAnalysis response = new PerformanceAnalysis(
                accuracy,
                game.getMistakes(),
                hints.size(),
                moves.size(),
                game.getElapsedSeconds(),
                strongArea,
                improvement,
                recommendation
        );
        response.setPredictedDifficulty(prediction.label());
        response.setModelConfidence(prediction.confidence());
        response.setHistoryCount((int) performanceRepository.count() + 1);

        performanceRepository.save(new PlayerPerformance(
                game.getId(), game.getDifficulty(), accuracy, game.getMistakes(), hints.size(),
                moves.size(), game.getElapsedSeconds(), prediction.label(), prediction.confidence()
        ));
        return response;
    }

    public List<PlayerPerformance> getHistory() {
        return performanceRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<PlayerPerformance> getHistoryForUser(Long userId) {
        if (userId == null) {
            return performanceRepository.findGuestPerformances();
        }
        return performanceRepository.findByUserId(userId);
    }

    private ModelPrediction predict(Game game, int accuracy, int moves, int hints) {
        List<PlayerPerformance> history = performanceRepository.findAllByOrderByCreatedAtDesc();
        List<double[]> featureRows = new ArrayList<>();
        List<double[]> labels = new ArrayList<>();
        addBootstrapData(featureRows, labels);

        for (PlayerPerformance item : history) {
            featureRows.add(features(item.getAccuracy(), item.getMistakes(), item.getHintsUsed(),
                    item.getMovesMade(), item.getElapsedSeconds(), item.getDifficulty()));
            labels.add(oneHot(labelIndex(item.getPredictedDifficulty())));
        }

        MultiLayerNetwork model = buildModel();
                INDArray trainingFeatures = Nd4j.create(featureRows.toArray(new double[0][]));
                INDArray trainingLabels = Nd4j.create(labels.toArray(new double[0][]));
                for (int epoch = 0; epoch < 20; epoch++) {
                        model.fit(trainingFeatures, trainingLabels);
                }

        INDArray output = model.output(Nd4j.create(new double[][] {
                features(accuracy, game.getMistakes(), hints, moves, game.getElapsedSeconds(), game.getDifficulty())
        }));
        int index = Nd4j.argMax(output, 1).getInt(0);
        return new ModelPrediction(LABELS[index], output.getDouble(0, index));
    }

    private MultiLayerNetwork buildModel() {
        MultiLayerConfiguration configuration = new NeuralNetConfiguration.Builder()
                .seed(42)
                .weightInit(WeightInit.XAVIER)
                .updater(new Adam(0.01))
                .list()
                .layer(new DenseLayer.Builder().nIn(6).nOut(12).activation(Activation.RELU).build())
                .layer(new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT)
                        .nIn(12).nOut(3).activation(Activation.SOFTMAX).build())
                .build();
        MultiLayerNetwork model = new MultiLayerNetwork(configuration);
        model.init();
        return model;
    }

    private void addBootstrapData(List<double[]> features, List<double[]> labels) {
        addBootstrapRow(features, labels, 98, 0, 1, 35, 240, "Easy");
        addBootstrapRow(features, labels, 92, 1, 3, 48, 600, "Medium");
        addBootstrapRow(features, labels, 78, 4, 8, 70, 1200, "Hard");
        addBootstrapRow(features, labels, 65, 8, 12, 85, 1800, "Hard");
    }

    private void addBootstrapRow(List<double[]> features, List<double[]> labels, int accuracy,
                                 int mistakes, int hints, int moves, long elapsed, String difficulty) {
        features.add(features(accuracy, mistakes, hints, moves, elapsed, difficulty));
        labels.add(oneHot(labelIndex(difficulty)));
    }

    private double[] features(int accuracy, int mistakes, int hints, int moves,
                              long elapsed, String difficulty) {
        double difficultyValue = "Hard".equalsIgnoreCase(difficulty) ? 1.0
                : "Medium".equalsIgnoreCase(difficulty) ? 0.5 : 0.0;
        return new double[] {
                accuracy / 100.0,
                Math.min(mistakes, 10) / 10.0,
                Math.min(hints, 12) / 12.0,
                Math.min(moves, 100) / 100.0,
                Math.min(elapsed, 1800) / 1800.0,
                difficultyValue
        };
    }

    private double[] oneHot(int index) {
        double[] values = new double[3];
        values[index] = 1.0;
        return values;
    }

    private int labelIndex(String label) {
        if ("Hard".equalsIgnoreCase(label)) return 2;
        if ("Medium".equalsIgnoreCase(label)) return 1;
        return 0;
    }

    private record ModelPrediction(String label, double confidence) {}
}
