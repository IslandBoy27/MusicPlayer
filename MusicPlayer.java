/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package musicplayer;

/**
 *
 * @author Jayma
 */
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

class Song implements Serializable {
    String title;
    String artist;
    String duration;
    String imagePath;
    String filePath;
    String genre;

    public Song(String title, String artist, String duration, String imagePath, String filePath, String genre) {
        this.title = title;
        this.artist = artist;
        this.duration = duration;
        this.imagePath = imagePath;
        this.filePath = filePath;
        this.genre = genre;
    }

    @Override
    public String toString() {
        return title + " - " + artist;
    }
}

public class MusicPlayer extends JFrame {
    private JButton playButton, pauseButton, nextButton, previousButton, stopButton, addMusicButton, showPlaylistButton;
    private JLabel songLabel;
    private JProgressBar progressBar;
    private Clip audioClip;
    private boolean isPlaying = false;
    private boolean isPaused = false;
    private long clipTimePosition = 0;
    private int currentSongIndex = 0;
    private ArrayList<Song> playlist = new ArrayList<>();
    private final String playlistFile = "playlist.bin";
    private Timer timer;
    private JLabel imageLabel;
    private JPopupMenu playlistMenu;

    public MusicPlayer() {
        setTitle("Reproductor de Música - WAV");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Panel de control
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BorderLayout());

        // Panel de progreso
        JPanel progressPanel = new JPanel();
        progressPanel.setLayout(new BorderLayout());

        // Barra de progreso
        progressBar = new JProgressBar();
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressPanel.add(progressBar, BorderLayout.CENTER);

        // Agregar el panel de progreso al panel de control
        controlPanel.add(progressPanel, BorderLayout.NORTH);

        // Panel de botones
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 15));

        // Botones
        playButton = new JButton(">");
        playButton.setFont(new Font("Arial", Font.BOLD, 24));

        pauseButton = new JButton("||");
        pauseButton.setFont(new Font("Arial", Font.BOLD, 24));

        previousButton = new JButton("<<");
        previousButton.setFont(new Font("Arial", Font.BOLD, 24));

        nextButton = new JButton(">>");
        nextButton.setFont(new Font("Arial", Font.BOLD, 24));

        stopButton = new JButton("■");
        stopButton.setFont(new Font("Arial", Font.BOLD, 24));

        addMusicButton = new JButton("+");
        addMusicButton.setFont(new Font("Arial", Font.BOLD, 24));

        showPlaylistButton = new JButton("♫");
        showPlaylistButton.setFont(new Font("Arial", Font.BOLD, 24));

        // Añadir botones al panel
        buttonPanel.add(previousButton);
        buttonPanel.add(playButton);
        buttonPanel.add(pauseButton);
        buttonPanel.add(nextButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(addMusicButton);
        buttonPanel.add(showPlaylistButton);

        // Inicialización del menú de lista de reproducción
        playlistMenu = new JPopupMenu();
        updatePlaylistMenu();

        showPlaylistButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Mostrar el menú un poco más arriba del botón
                Point location = showPlaylistButton.getLocationOnScreen();
                playlistMenu.show(showPlaylistButton, 0, -playlistMenu.getPreferredSize().height - 10);
            }
        });

        // Añadir el panel de botones al panel de control
        controlPanel.add(buttonPanel, BorderLayout.CENTER);

        // Etiqueta de la canción
        songLabel = new JLabel("No song playing");
        songLabel.setHorizontalAlignment(SwingConstants.CENTER);
        songLabel.setFont(new Font("Arial", Font.PLAIN, 16));

        add(songLabel, BorderLayout.NORTH);

        // Panel de imagen
        imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setPreferredSize(new Dimension(300, 300));
        add(imageLabel, BorderLayout.CENTER);

        // Añadir el panel de control a la ventana principal
        add(controlPanel, BorderLayout.SOUTH);

        // Action listener para el botón de Play
        playButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!isPlaying && !playlist.isEmpty()) {
                    playSong(playlist.get(currentSongIndex));
                } else if (isPaused) {
                    resumeSong();
                }
            }
        });

        // Action listener para el botón de Pause
        pauseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pauseSong();
            }
        });

        // Action listener para el botón de Siguiente
        nextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                nextSong();
            }
        });

        // Action listener para el botón de Anterior
        previousButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                previousSong();
            }
        });

        // Action listener para el botón de Parar
        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopSong();
            }
        });

        // Action listener para el botón de Agregar Música
        addMusicButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addMusic();
            }
        });

        // Cargar la lista de canciones al iniciar
        loadPlaylist();

        setVisible(true);
    }

    private void playSong(Song song) {
        try {
            if (audioClip != null && audioClip.isRunning()) {
                audioClip.stop();
            }

            File audioFile = new File(song.filePath);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
            audioClip = AudioSystem.getClip();
            audioClip.open(audioStream);

            // Reiniciar barra de progreso y temporizador
            resetProgressBar();

            int totalMilliseconds = (int) (audioClip.getMicrosecondLength() / 1000);
            progressBar.setMaximum(totalMilliseconds);
            progressBar.setString(formatTime(0) + " / " + formatTime(totalMilliseconds));

            // Temporizador para actualizar la barra de progreso
            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (isPlaying) {
                        SwingUtilities.invokeLater(() -> {
                            long currentMilliseconds = audioClip.getMicrosecondPosition() / 1000;
                            progressBar.setValue((int) currentMilliseconds);
                            progressBar.setString(formatTime((int) currentMilliseconds) + " / " + formatTime(totalMilliseconds));
                        });
                    }
                }
            }, 0, 1000);

            audioClip.start();
            isPlaying = true;
            isPaused = false;
            songLabel.setText("Reproduciendo: " + song.title + " (" + song.artist + ") - Genre: " + song.genre);

            // Mostrar la imagen del álbum
            if (song.imagePath != null && !song.imagePath.isEmpty()) {
                ImageIcon imageIcon = new ImageIcon(song.imagePath);
                Image image = imageIcon.getImage().getScaledInstance(300, 300, Image.SCALE_SMOOTH);
                imageLabel.setIcon(new ImageIcon(image));
            } else {
                imageLabel.setIcon(null);
            }

            // Ocultar el menú de lista de reproducción
            playlistMenu.setVisible(false);
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    private void pauseSong() {
        if (isPlaying && !isPaused) {
            audioClip.stop();
            clipTimePosition = audioClip.getMicrosecondPosition();
            isPaused = true;
        }
    }

    private void resumeSong() {
        if (isPaused) {
            audioClip.setMicrosecondPosition(clipTimePosition);
            audioClip.start();
            isPlaying = true;
            isPaused = false;
        }
    }

    private void stopSong() {
        if (isPlaying) {
            audioClip.stop();
            audioClip.setMicrosecondPosition(0);
            progressBar.setValue(0);
            progressBar.setString(formatTime(0) + " / " + formatTime(progressBar.getMaximum()));
            songLabel.setText("No song playing");
            isPlaying = false;
            isPaused = false;
            if (timer != null) {
                timer.cancel();
                timer = null;
            }
        }
    }

    private void nextSong() {
        if (!playlist.isEmpty()) {
            currentSongIndex = (currentSongIndex + 1) % playlist.size();
            playSong(playlist.get(currentSongIndex));
        }
    }

    private void previousSong() {
        if (!playlist.isEmpty()) {
            currentSongIndex = (currentSongIndex - 1 + playlist.size()) % playlist.size();
            playSong(playlist.get(currentSongIndex));
        }
    }

    private void addMusic() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Audio Files", "wav"));
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File audioFile = fileChooser.getSelectedFile();
            String filePath = audioFile.getAbsolutePath();

            // Obtener información adicional
            String title = JOptionPane.showInputDialog("Título de la canción:");
            String artist = JOptionPane.showInputDialog("Artista:");
            String genre = JOptionPane.showInputDialog("Género:");

            // Seleccionar imagen
            JFileChooser imageChooser = new JFileChooser();
            imageChooser.setFileFilter(new FileNameExtensionFilter("Image Files", "jpg", "jpeg", "png"));
            int imageResult = imageChooser.showOpenDialog(this);
            String imagePath = "";
            if (imageResult == JFileChooser.APPROVE_OPTION) {
                File imageFile = imageChooser.getSelectedFile();
                imagePath = imageFile.getAbsolutePath();
            }

            // Crear y agregar la canción
            Song newSong = new Song(title, artist, "0:00", imagePath, filePath, genre);
            playlist.add(newSong);
            updatePlaylistMenu();
            savePlaylist();
        }
    }

    private void updatePlaylistMenu() {
        playlistMenu.removeAll();
        for (Song song : playlist) {
            JMenuItem menuItem = new JMenuItem(song.toString());
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    playSong(song);
                }
            });
            playlistMenu.add(menuItem);
        }
    }

    private void savePlaylist() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(playlistFile))) {
            oos.writeObject(playlist);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadPlaylist() {
        File file = new File(playlistFile);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                playlist = (ArrayList<Song>) ois.readObject();
                updatePlaylistMenu();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void resetProgressBar() {
        if (timer != null) {
            timer.cancel();
        }
        progressBar.setValue(0);
        progressBar.setString("0:00 / " + formatTime(progressBar.getMaximum()));
    }

    private String formatTime(int milliseconds) {
        int seconds = (milliseconds / 1000) % 60;
        int minutes = (milliseconds / (1000 * 60)) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MusicPlayer::new);
    }
}
