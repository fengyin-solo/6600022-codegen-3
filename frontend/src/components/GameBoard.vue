<template>
  <div class="flex flex-col items-center">
    <canvas
      ref="canvasRef"
      :width="canvasSize"
      :height="canvasSize"
      :class="store.status === 'spectating' ? 'cursor-default' : 'cursor-pointer'"
      class="rounded-lg shadow-2xl"
      @click="handleClick"
    />
    <div v-if="store.status === 'spectating'" class="mt-2 flex items-center gap-2 text-sm">
      <span class="inline-block w-2 h-2 rounded-full bg-red-500 animate-pulse"></span>
      <span class="text-red-400">观战模式 — 无法落子</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue';
import { useGameStore } from '../store/game';

const store = useGameStore();
const canvasRef = ref<HTMLCanvasElement | null>(null);

const BOARD_SIZE = 15;
const CELL_SIZE = 40;
const PADDING = 30;
const canvasSize = CELL_SIZE * (BOARD_SIZE - 1) + PADDING * 2;

const boardData = computed(() => {
  if (store.status === 'replaying') return store.replayBoard;
  if (store.status === 'spectating') return store.spectatingBoard;
  return store.board;
});

const movesList = computed(() => {
  if (store.status === 'replaying') return store.replayMoves.slice(0, store.replayIndex);
  if (store.status === 'spectating') return store.spectatingMoves;
  return store.moves;
});

const showMoveNumbers = computed(() => store.status === 'spectating');

function drawBoard(ctx: CanvasRenderingContext2D) {
  ctx.fillStyle = '#1a3a2a';
  ctx.fillRect(0, 0, canvasSize, canvasSize);

  ctx.strokeStyle = '#3d7a5a';
  ctx.lineWidth = 1;

  for (let i = 0; i < BOARD_SIZE; i++) {
    const pos = PADDING + i * CELL_SIZE;
    ctx.beginPath();
    ctx.moveTo(PADDING, pos);
    ctx.lineTo(PADDING + (BOARD_SIZE - 1) * CELL_SIZE, pos);
    ctx.stroke();

    ctx.beginPath();
    ctx.moveTo(pos, PADDING);
    ctx.lineTo(pos, PADDING + (BOARD_SIZE - 1) * CELL_SIZE);
    ctx.stroke();
  }

  const starPoints = [[3, 3], [3, 11], [11, 3], [11, 11], [7, 7]];
  ctx.fillStyle = '#5aaa7a';
  for (const [r, c] of starPoints) {
    ctx.beginPath();
    ctx.arc(PADDING + c * CELL_SIZE, PADDING + r * CELL_SIZE, 4, 0, Math.PI * 2);
    ctx.fill();
  }
}

function drawStone(ctx: CanvasRenderingContext2D, row: number, col: number, player: number, isLast: boolean, moveNumber?: number) {
  const x = PADDING + col * CELL_SIZE;
  const y = PADDING + row * CELL_SIZE;
  const radius = CELL_SIZE * 0.42;

  const gradient = ctx.createRadialGradient(x - 3, y - 3, 2, x, y, radius);
  if (player === 1) {
    gradient.addColorStop(0, '#555');
    gradient.addColorStop(1, '#111');
  } else {
    gradient.addColorStop(0, '#fff');
    gradient.addColorStop(1, '#ccc');
  }

  ctx.beginPath();
  ctx.arc(x, y, radius, 0, Math.PI * 2);
  ctx.fillStyle = gradient;
  ctx.fill();

  ctx.strokeStyle = player === 1 ? '#000' : '#aaa';
  ctx.lineWidth = 1;
  ctx.stroke();

  if (showMoveNumbers.value && moveNumber !== undefined) {
    ctx.fillStyle = player === 1 ? '#eee' : '#222';
    ctx.font = `bold ${CELL_SIZE * 0.28}px sans-serif`;
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(String(moveNumber), x, y + 1);
  } else if (isLast) {
    ctx.beginPath();
    ctx.arc(x, y, 5, 0, Math.PI * 2);
    ctx.fillStyle = '#ef4444';
    ctx.fill();
  }
}

function buildMoveIndexMap(moves: { row: number; col: number }[]): Map<string, number> {
  const map = new Map<string, number>();
  for (let i = 0; i < moves.length; i++) {
    const key = `${moves[i].row},${moves[i].col}`;
    map.set(key, i + 1);
  }
  return map;
}

function render() {
  const canvas = canvasRef.value;
  if (!canvas) return;
  const ctx = canvas.getContext('2d');
  if (!ctx) return;

  drawBoard(ctx);

  const currentBoard = boardData.value;
  const currentMoves = movesList.value;
  const lastMove = currentMoves.length > 0 ? currentMoves[currentMoves.length - 1] : null;
  const moveIndexMap = showMoveNumbers.value ? buildMoveIndexMap(currentMoves) : null;

  for (let r = 0; r < BOARD_SIZE; r++) {
    for (let c = 0; c < BOARD_SIZE; c++) {
      if (currentBoard[r][c] !== 0) {
        const isLast = lastMove !== null && lastMove.row === r && lastMove.col === c;
        const key = `${r},${c}`;
        const moveNumber = moveIndexMap ? moveIndexMap.get(key) : undefined;
        drawStone(ctx, r, c, currentBoard[r][c], isLast, moveNumber);
      }
    }
  }
}

function handleClick(e: MouseEvent) {
  if (store.status !== 'playing') return;
  if (store.aiConfig.enabled && store.currentPlayer === store.aiConfig.playerColor) return;

  const canvas = canvasRef.value;
  if (!canvas) return;

  const rect = canvas.getBoundingClientRect();
  const mx = e.clientX - rect.left;
  const my = e.clientY - rect.top;

  const col = Math.round((mx - PADDING) / CELL_SIZE);
  const row = Math.round((my - PADDING) / CELL_SIZE);

  if (row < 0 || row >= BOARD_SIZE || col < 0 || col >= BOARD_SIZE) return;

  const placed = store.placeStone(row, col);
  if (placed && store.aiConfig.enabled && !store.isGameOver) {
    setTimeout(() => store.aiMove(), 200);
  }
}

onMounted(() => render());
watch([boardData, () => store.replayIndex, () => store.status, () => store.spectatingMoves.length], () => render());
</script>
