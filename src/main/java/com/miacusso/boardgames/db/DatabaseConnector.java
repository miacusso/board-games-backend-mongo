package com.miacusso.boardgames.db;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.types.ObjectId;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class DatabaseConnector {

	private MongoDatabase database;

	public DatabaseConnector() {
		// "mongodb+srv://<username>:<password>@<cluster-address>/test?w=majority"
		ConnectionString connectionString = new ConnectionString("mongodb://localhost:27017");
		CodecRegistry pojoCodecRegistry = fromProviders(PojoCodecProvider.builder().automatic(true).build());
		CodecRegistry codecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
                pojoCodecRegistry);
		MongoClientSettings settings = MongoClientSettings.builder()
				.applyConnectionString(connectionString)
				.codecRegistry(codecRegistry)
				.retryWrites(true)
				.build();
		MongoClient client = MongoClients.create(settings);
		List<String> databases = client.listDatabaseNames().into(new ArrayList<String>());

		this.database = client.getDatabase("board-games");

		if (!databases.contains("board-games")) {
			MongoCollection<Document> games = this.database.getCollection("games");
			Document doc1 = new Document("_id", 1).append("name", "TIC TAC TOE");
			Document doc2 = new Document("_id", 2).append("name", "FOUR IN LINE");
			List<Document> gameDocumentsList = new ArrayList<Document>();
			gameDocumentsList.add(doc1);
			gameDocumentsList.add(doc2);
			games.insertMany(gameDocumentsList);

			MongoCollection<Document> players = this.database.getCollection("players");
			Document doc3 = new Document("_id", 1).append("name", "X").append("game", 1);
			Document doc4 = new Document("_id", 2).append("name", "O").append("game", 1);
			Document doc5 = new Document("_id", 3).append("name", "RED").append("game", 2);
			Document doc6 = new Document("_id", 4).append("name", "BLUE").append("game", 2);
			List<Document> playerDocumentsList = new ArrayList<Document>();
			playerDocumentsList.add(doc3);
			playerDocumentsList.add(doc4);
			playerDocumentsList.add(doc5);
			playerDocumentsList.add(doc6);
			players.insertMany(playerDocumentsList);
		}
	}

	public void insertGameResult(Integer winner, Integer game) {
		GameResultDBO result = new GameResultDBO();
		result.setId(new ObjectId().getTimestamp());
		result.setDate(LocalDate.now());
		result.setWinner(winner);
		result.setGame(game);
		this.insertGameResult(result);
	}

	public void insertGameResult(GameResultDBO gameResult) {
		this.database.getCollection("results", GameResultDBO.class).insertOne(gameResult);
	}

	public List<PlayerDBO> retrievePlayersForGame(GameDBO game) {
		Document filter = new Document().append("game", game.getId());
		return this.database.getCollection("players", PlayerDBO.class).find(filter).into(new ArrayList<PlayerDBO>());
	}

	public Map<String, Integer> retrieveResultsCountForGame(GameDBO game) {

		List<PlayerDBO> players = this.retrievePlayersForGame(game);
		Map<PlayerDBO, Integer> responseMap = new HashMap<PlayerDBO, Integer>();
		for (PlayerDBO playerDBO : players) {
			responseMap.put(playerDBO, 0);
		}

		Document filter = new Document().append("game", game.getId());
		List<GameResultDBO> results = this.database.getCollection("results", GameResultDBO.class).find(filter).into(new ArrayList<GameResultDBO>());
		for (GameResultDBO result : results) {
			PlayerDBO playerDBO = players.stream().filter(player -> result.getWinner().equals(player.getId())).findFirst().get();
			responseMap.put(playerDBO, responseMap.get(playerDBO) + 1);
		}

		return responseMap.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().getName(), e -> e.getValue()));

	}

	public void removeResultsForGame(GameDBO game) {
		Document filter = new Document().append("game", game.getId());
		this.database.getCollection("results").deleteMany(filter);
	}

}
