#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}Starting Git Deployment...${NC}"

# Check if there are changes to commit
if [[ -z $(git status -s) ]]; then
    echo "No changes to commit"
    exit 0
fi

# Stage all changes
echo -e "${BLUE}Staging changes...${NC}"
git add .

# Show what will be committed
echo -e "${BLUE}Changes to be committed:${NC}"
git status --short

# Commit with message
echo -e "${BLUE}Committing changes...${NC}"
git commit -m "feat: Add GPS location tracking endpoint for users

- Add PUT /api/users/{id}/location endpoint
- Add LocationUpdateRequest DTO with GPS coordinates validation
- Support real-time location updates with timestamp
- Validate latitude (-90 to 90) and longitude (-180 to 180)"

# Push to remote
echo -e "${BLUE}Pushing to remote repository...${NC}"
git push origin main

echo -e "${GREEN}Deployment completed successfully!${NC}"
