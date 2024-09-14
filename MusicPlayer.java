package musicplayer;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.advanced.AdvancedPlayer;

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
    String duration; // In mm:ss format
    String imagePath;
    String filePath;
    String genre; // Added genre information

    public Song(String title, String artist, String duration, String imagePath, String filePath, String genre) {
        this.title = title;
        this.artist = artist;
        this.duration = duration;
        this.imagePath = imagePath;
        this.filePath = filePath;
        this.genre = genre; // Set genre
    }
}

public class MusicPlayer extends JFrame {
    private JButton playButton, pauseButton, nextButton, previousButton, addMusicButton, selectButton, stopButton;
    private JLabel songLabel;
    private JProgressBar progressBar;
    private AdvancedPlayer mp3Player;
    private boolean isPlaying = false;
    private boolean isPaused = false;
    private long clipTimePosition = 0;
    private int currentSongIndex = 0;
    private ArrayList<Song> playlist = new ArrayList<>();
    private final String playlistFile = "playlist.bin";
    private Timer timer;
    private JLabel imageLabel; // To display album image

    public MusicPlayer() {
        setTitle("Reproductor de Música - MP3");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BorderLayout());

        JPanel progressPanel = new JPanel();
        progressPanel.setLayout(new BorderLayout());

        progressBar = new JProgressBar();
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(400, 20));
        progressPanel.add(progressBar, BorderLayout.CENTER);
        controlPanel.add(progressPanel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 15));

        playButton = new JButton(">"); 
        pauseButton = new JButton("||"); 
        previousButton = new JButton("<<"); 
        nextButton = new JButton(">>"); 
        addMusicButton = new JButton("+");
        selectButton = new JButton("Select");
        stopButton = new JButton("Stop");

        buttonPanel.add(previousButton);
        buttonPanel.add(playButton);
        buttonPanel.add(pauseButton);
        buttonPanel.add(nextButton);
        buttonPanel.add(addMusicButton);
        buttonPanel.add(selectButton);
        buttonPanel.add(stopButton);

        controlPanel.add(buttonPanel, BorderLayout.CENTER);

        songLabel = new JLabel("No song playing");
        songLabel.setHorizontalAlignment(SwingConstants.CENTER);
        songLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        add(songLabel, BorderLayout.NORTH);

        imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(imageLabel, BorderLayout.CENTER); // Add imageLabel to center

        add(controlPanel, BorderLayout.SOUTH);

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

        pauseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pauseSong();
            }
        });

        nextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                nextSong();
            }
        });

        previousButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                previousSong();
            }
        });

        addMusicButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addMusic();
            }
        });

        selectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectSong();
            }
        });

        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopSong();
            }
        });

        loadPlaylist();
        setVisible(true);
    }

    private void playSong(Song song) {
        try {
            if (mp3Player != null && isPlaying) {
                mp3Player.close();
            }

            FileInputStream fileInputStream = new FileInputStream(song.filePath);
            mp3Player = new AdvancedPlayer(fileInputStream);
            progressBar.setValue(0);
            // Set duration for progress bar max
            int totalMilliseconds = getDuration(song.filePath);
            progressBar.setMaximum(totalMilliseconds);
            progressBar.setString(formatTime(0) + " / " + formatTime(totalMilliseconds));

            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (isPlaying) {
                        SwingUtilities.invokeLater(() -> {
                            long currentMilliseconds = getPosition(); // Update accordingly
                            progressBar.setValue((int) currentMilliseconds);
                            progressBar.setString(formatTime((int) currentMilliseconds) + " / " + formatTime(totalMilliseconds));
                        });
                    }
                }
            }, 0, 1000);

            isPlaying = true;
            isPaused = false;
            songLabel.setText("Reproduciendo: " + song.title);

            // Display image
            if (song.imagePath != null && !song.imagePath.isEmpty()) {
                ImageIcon imageIcon = new ImageIcon(song.imagePath);
                Image image = imageIcon.getImage().getScaledInstance(150, 150, Image.SCALE_SMOOTH); // Adjust size as needed
                imageLabel.setIcon(new ImageIcon(image));
            }

            new Thread(() -> {
                try {
                    mp3Player.play();
                } catch (JavaLayerException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (FileNotFoundException | JavaLayerException e) {
            e.printStackTrace();
        }
    }

    private void pauseSong() {
        if (isPlaying) {
            clipTimePosition = getPosition(); // Store current position
            mp3Player.close();
            isPaused = true;
            timer.cancel();
        }
    }

    private void resumeSong() {
        if (isPaused) {
            playSong(playlist.get(currentSongIndex)); // Call playSong to continue
            isPaused = false;
        }
    }

    private void nextSong() {
        if (mp3Player != null && isPlaying) {
            mp3Player.close();
            timer.cancel();
        }
        currentSongIndex = (currentSongIndex + 1) % playlist.size();
        playSong(playlist.get(currentSongIndex));
    }

    private void previousSong() {
        if (mp3Player != null && isPlaying) {
            mp3Player.close();
            timer.cancel();
        }
        currentSongIndex = (currentSongIndex - 1 + playlist.size()) % playlist.size();
        playSong(playlist.get(currentSongIndex));
    }

    private void addMusic() {
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Archivos MP3", "mp3");
        fileChooser.setFileFilter(filter);

        int returnValue = fileChooser.showOpenDialog(null);

        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String filePath = selectedFile.getAbsolutePath();

            // Gather additional information from user
            String title = JOptionPane.showInputDialog("Ingrese el nombre de la canción:");
            String artist = JOptionPane.showInputDialog("Ingrese el nombre del artista:");
            String genre = JOptionPane.showInputDialog("Ingrese el género de la música:");
            String imagePath = JOptionPane.showInputDialog("Ingrese la ruta de la imagen del álbum (deje en blanco si no tiene):");

            // Set default values if user does not provide them
            if (title == null || title.trim().isEmpty()) title = selectedFile.getName().replace(".mp3", "");
            if (artist == null || artist.trim().isEmpty()) artist = "Unknown Artist";
            if (genre == null || genre.trim().isEmpty()) genre = "Unknown Genre";
            if (imagePath == null) imagePath = "";

            String duration = formatDuration(getDuration(filePath)); // Duration

            Song song = new Song(title, artist, duration, imagePath, filePath, genre);
            playlist.add(song);
            savePlaylist();
            songLabel.setText("Agregado: " + selectedFile.getName());

            // Display image if available
            if (!imagePath.isEmpty()) {
                ImageIcon imageIcon = new ImageIcon(imagePath);
                Image image = imageIcon.getImage().getScaledInstance(150, 150, Image.SCALE_SMOOTH); // Adjust size as needed
                imageLabel.setIcon(new ImageIcon(image));
            } else {
                imageLabel.setIcon(null); // Clear image if no path
            }
        }
    }

    private void selectSong() {
        if (playlist.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay canciones en la lista de reproducción.");
            return;
        }

        String[] options = new String[playlist.size()];
        for (int i = 0; i < playlist.size(); i++) {
            options[i] = playlist.get(i).title;
        }

        String selectedTitle = (String) JOptionPane.showInputDialog(this, "Seleccione una canción:",
                "Seleccionar Canción", JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        if (selectedTitle != null) {
            for (int i = 0; i < playlist.size(); i++) {
                if (playlist.get(i).title.equals(selectedTitle)) {
                    currentSongIndex = i;
                    playSong(playlist.get(i));
                    break;
                }
            }
        }
    }

    private void stopSong() {
        if (mp3Player != null && isPlaying) {
            mp3Player.close();
            isPlaying = false;
            isPaused = false;
            timer.cancel();
            progressBar.setValue(0);
            progressBar.setString("0:00 / " + formatTime(getDuration(playlist.get(currentSongIndex).filePath)));
            songLabel.setText("Reproducción detenida");
            imageLabel.setIcon(null); // Clear image when stopped
        }
    }

    private void loadPlaylist() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(playlistFile))) {
            playlist = (ArrayList<Song>) ois.readObject();
        } catch (FileNotFoundException e) {
            // No playlist file, ignore
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void savePlaylist() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(playlistFile))) {
            oos.writeObject(playlist);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int getDuration(String filePath) {
        // Implement duration extraction here
        return 0; // Placeholder, return actual duration in milliseconds
    }

    private long getPosition() {
        // Implement position retrieval here
        return 0; // Placeholder, return actual position in milliseconds
    }

    private String formatDuration(int milliseconds) {
        int minutes = milliseconds / 60000;
        int seconds = (milliseconds % 60000) / 1000;
        return String.format("%d:%02d", minutes, seconds);
    }

    private String formatTime(int milliseconds) {
        int minutes = milliseconds / 60000;
        int seconds = (milliseconds % 60000) / 1000;
        return String.format("%d:%02d", minutes, seconds);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MusicPlayer());
    }
}
