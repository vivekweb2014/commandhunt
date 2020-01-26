const api = process.env.REACT_APP_API_URL || "http://localhost:8080/api";

localStorage.token = localStorage.token || Math.random().toString(36).substring(2);

const headers = {
    Accept: "application/json",
    Authorization: localStorage.token,
    "Content-Type": "application/json"
};

export const getAllCommands = () => fetch(`${api}/command`, { headers }).then(res => res.json());

export const getMatchingCommands = query => {
    query = encodeURIComponent(query);
    return fetch(`${api}/command/search?query=${query}`, { headers }).then(res => res.json());
};

export const getCommand = commandName => {
    commandName = encodeURIComponent(commandName);
    return fetch(`${api}/command/search?name=${commandName}`, { headers }).then(res => res.json())
};