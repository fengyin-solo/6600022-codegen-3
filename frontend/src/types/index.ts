export type BoardState = number[][];

export interface Move {
  row: number;
  col: number;
  player: number;
  timestamp: number;
}

export interface GameRecord {
  id: string;
  moves: Move[];
  winner: number | null;
  createdAt: string;
  duration: number;
}

export interface AIConfig {
  depth: number;
  enabled: boolean;
  playerColor: number;
}

export interface OngoingGame {
  id: string;
  currentPlayer: number;
  moves: Move[];
  winner: number | null;
  createdAt: string;
}

export type GameStatus = 'idle' | 'playing' | 'finished' | 'replaying' | 'spectating';
