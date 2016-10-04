package i5.las2peer.services.codeGenerationService.adapters;

import i5.las2peer.services.codeGenerationService.exception.GitHostException;

public interface GitHostAdapter {
	public void createRepo(String name, String description) throws GitHostException;
	public void deleteRepo(String name) throws GitHostException;
}