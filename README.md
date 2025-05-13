How to run the project?

Requirements - 
1. Generate a symmetric key `fileKey.txt` from `fernet.KeyGenerator` class.
2. Generate a public & private key for the users participating from `rsa.KeyGenerator` class. (e.g. `user1.txt`, `user2.txt`)
3. Run the users (`user1`, `user2`) you want to share information b/w in seprate terminal as not to clutter them with console messages. See `makefile`.
4. Create a test file inside `files/` directory, if no such file exist. (e.g. `test.txt`).

Start - 
1. follow `makefile` to run mutiple nodes in one teminal and two nodes we will be working with in two seprate terminal.
   - terminal - 1 `make run-nodes`
   - terminal - 2 `make run-user1`  (make sure keys for user1 exist in `user1.txt`)
   - terminal - 3 `make run-user2`
2. Default time for election is every two minutes, wait for first election to complete.
3. Available commands
   - `exit - exit the program`
   - `upload <filePath> - Upload a file`
   - `share <blockIndex> <receiverId> [fileName - optional] [fileHash - optional] - Share a file with another node`
   - `download <blockIndex> - To download a file`
     
Note - if you upload the file and it makes the block on index 1, `blockIndex = 1`. Since, it's the root block of file.

4. Flow -> 
   -  `user1` uploads a file, proposes a block and after validation creates a first block.
   -  `user1` can now share the file information to other user (`user2`) using `blockIndex = 1`.
   -  `user1` can download a file using same root block index.
