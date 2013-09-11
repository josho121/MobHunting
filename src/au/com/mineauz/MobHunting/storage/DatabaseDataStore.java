package au.com.mineauz.MobHunting.storage;

import java.sql.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public abstract class DatabaseDataStore implements DataStore
{
	protected Connection mConnection;
	
	/**
	 * Args: player id
	 */
	protected PreparedStatement mAddPlayerStatsStatement;

	/**
	 * Args: player id, achievement, date, progress
	 */
	protected PreparedStatement mRecordAchievementStatement;

	/**
	 * Args: player name
	 */
	protected PreparedStatement mAddPlayerStatement;
	/**
	 * Args: player name
	 */
	protected PreparedStatement[] mGetPlayerStatement;
	
	/**
	 * Args: player id
	 */
	protected PreparedStatement mLoadAchievementsStatement;
	
	@Override
	public void initialize() throws DataStoreException
	{
		try
		{
			
			mConnection = setupConnection();
			mConnection.setAutoCommit(false);
			
			setupTables(mConnection);
			
			mGetPlayerStatement = new PreparedStatement[4];
			setupStatements(mConnection);
		}
		catch(SQLException e)
		{
			throw new DataStoreException(e);
		}
	}
	
	protected abstract Connection setupConnection() throws SQLException, DataStoreException;
	protected abstract void setupTables(Connection connection) throws SQLException;
	protected abstract void setupStatements(Connection connection) throws SQLException;
	
	protected void rollback() throws DataStoreException
	{
		try
		{
			mConnection.rollback();
		}
		catch(SQLException e)
		{
			throw new DataStoreException(e);
		}
	}

	@Override
	public void shutdown() throws DataStoreException
	{
		try
		{
			mConnection.close();
		}
		catch ( SQLException e )
		{
			throw new DataStoreException(e);
		}
	}
	
	protected Map<String, Integer> getPlayerIds(Set<String> players) throws SQLException
	{
		mAddPlayerStatement.clearBatch();
		
		for(String player : players)
		{
			mAddPlayerStatement.setString(1, player);
			mAddPlayerStatement.addBatch();
		}
		mAddPlayerStatement.executeBatch();
		
		int left = players.size();
		Iterator<String> it = players.iterator();
		HashMap<String, Integer> ids = new HashMap<String, Integer>();
		
		while(left > 0)
		{
			PreparedStatement statement;
			int size = 0;
			if(left >= 10)
			{
				size = 10;
				statement = mGetPlayerStatement[3];
			}
			else if(left >= 5)
			{
				size = 5;
				statement = mGetPlayerStatement[2];
			}
			else if(left >= 2)
			{
				size = 2;
				statement = mGetPlayerStatement[1];
			}
			else
			{
				size = 1;
				statement = mGetPlayerStatement[0];
			}
			
			left -= size;
			
			for(int i = 0; i < size; ++i)
				statement.setString(i + 1, it.next());

			ResultSet results = statement.executeQuery();
			
			while(results.next())
				ids.put(results.getString(1), results.getInt(2));
		}
		
		return ids;
	}
	
	protected int getPlayerId(String playerName) throws SQLException, DataStoreException
	{
		mGetPlayerStatement[0].setString(1, playerName);
		ResultSet result = mGetPlayerStatement[0].executeQuery();
		
		if(result.next())
			return result.getInt(2);
		
		throw new UserNotFoundException("User " + playerName + " is not present in database");
	}
	
	@Override
	public Set<AchievementStore> loadAchievements( String player ) throws DataStoreException
	{
		try
		{
			int playerId = getPlayerId(player);
			
			mLoadAchievementsStatement.setInt(1, playerId);
			
			ResultSet set = mLoadAchievementsStatement.executeQuery();
			HashSet<AchievementStore> achievements = new HashSet<AchievementStore>();
			
			while(set.next())
			{
				// TODO: Date is not used. col 2
				achievements.add(new AchievementStore(set.getString(1), player, set.getInt(3)));
			}
			
			return achievements;
		}
		catch(SQLException e)
		{
			throw new DataStoreException(e);
		}
	}
	
	@Override
	public void saveAchievements( Set<AchievementStore> achievements ) throws DataStoreException
	{
		try
		{
			HashSet<String> names = new HashSet<String>();
			for(AchievementStore achievement : achievements)
				names.add(achievement.playerName);
			
			Map<String, Integer> ids = getPlayerIds(names);
			
			for(AchievementStore achievement : achievements)
			{
				mRecordAchievementStatement.setInt(1, ids.get(achievement.playerName));
				mRecordAchievementStatement.setString(2, achievement.id);
				mRecordAchievementStatement.setDate(3, new Date(System.currentTimeMillis()));
				mRecordAchievementStatement.setInt(4, achievement.progress);
				
				mRecordAchievementStatement.addBatch();
			}

			mRecordAchievementStatement.executeBatch();
			
			mConnection.commit();
		}
		catch(SQLException e)
		{
			rollback();
			throw new DataStoreException(e);
		}
	}
}
