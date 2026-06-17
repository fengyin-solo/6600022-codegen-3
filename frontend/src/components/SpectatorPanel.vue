<template>
  <div class="bg-gray-900 rounded-xl p-4 border border-gray-700">
    <div class="flex items-center justify-between mb-3">
      <h3 class="text-lg font-bold text-green-400">观战大厅</h3>
      <button
        @click="refreshGames"
        class="p-1.5 bg-gray-800 rounded-lg hover:bg-gray-700 text-gray-400 transition-colors"
        title="刷新"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
        </svg>
      </button>
    </div>

    <div v-if="store.status === 'spectating'">
      <div class="space-y-3">
        <div class="flex items-center gap-2">
          <span class="inline-block w-2 h-2 rounded-full bg-red-500 animate-pulse"></span>
          <span class="text-sm text-red-400 font-medium">正在观战</span>
        </div>

        <div class="space-y-2 text-sm">
          <div class="flex justify-between">
            <span class="text-gray-400">当前回合</span>
            <span class="flex items-center gap-1">
              <span class="inline-block w-3 h-3 rounded-full" :class="store.spectatingCurrentPlayer === 1 ? 'bg-gray-800 border border-gray-600' : 'bg-white'"></span>
              {{ store.spectatingCurrentPlayer === 1 ? '黑棋' : '白棋' }}
            </span>
          </div>
          <div class="flex justify-between">
            <span class="text-gray-400">手数</span>
            <span class="text-white">{{ store.spectatingMoveCount }}</span>
          </div>
          <div v-if="store.spectatingWinner !== null" class="flex justify-between">
            <span class="text-gray-400">结果</span>
            <span class="font-bold" :class="store.spectatingWinner === 1 ? 'text-gray-300' : store.spectatingWinner === 2 ? 'text-white' : 'text-yellow-400'">
              {{ store.spectatingWinner === 1 ? '黑棋胜' : store.spectatingWinner === 2 ? '白棋胜' : '平局' }}
            </span>
          </div>
        </div>

        <div v-if="store.spectatingMoves.length > 0" class="bg-gray-800 rounded-lg p-3 max-h-48 overflow-y-auto">
          <div class="text-xs text-gray-500 mb-2">落子顺序</div>
          <div class="space-y-1">
            <div
              v-for="(move, idx) in store.spectatingMoves"
              :key="idx"
              class="flex items-center gap-2 text-xs"
            >
              <span class="w-5 text-right text-gray-500">{{ idx + 1 }}.</span>
              <span class="inline-block w-2.5 h-2.5 rounded-full" :class="move.player === 1 ? 'bg-gray-600' : 'bg-white'"></span>
              <span class="text-gray-300">{{ coordLabel(move.row, move.col) }}</span>
            </div>
          </div>
        </div>

        <button
          @click="store.stopSpectating()"
          class="w-full py-2 bg-red-600/20 border border-red-600/50 text-red-400 rounded-lg hover:bg-red-600/30 transition-colors text-sm"
        >
          退出观战
        </button>
      </div>
    </div>

    <div v-else>
      <p v-if="store.ongoingGames.length === 0" class="text-gray-500 text-sm">暂无进行中的对局</p>
      <div v-else class="space-y-2 max-h-64 overflow-y-auto">
        <div
          v-for="game in store.ongoingGames"
          :key="game.id"
          class="bg-gray-800 rounded-lg p-3 border border-gray-700 hover:border-green-600/50 transition-colors"
        >
          <div class="flex justify-between items-center mb-2">
            <span class="text-sm text-gray-300">{{ game.createdAt }}</span>
            <span class="text-xs px-2 py-0.5 rounded-full bg-green-600/20 text-green-400">
              进行中
            </span>
          </div>
          <div class="flex justify-between items-center">
            <div class="text-xs text-gray-500">
              {{ game.moves.length }} 手 ·
              <span class="inline-block w-2 h-2 rounded-full align-middle" :class="game.currentPlayer === 1 ? 'bg-gray-600' : 'bg-white'"></span>
              {{ game.currentPlayer === 1 ? '黑棋' : '白棋' }}执手
            </div>
            <button
              @click="store.startSpectating(game.id)"
              class="px-3 py-1 text-xs bg-green-600 hover:bg-green-500 text-white rounded-lg transition-colors"
            >
              观战
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue';
import { useGameStore } from '../store/game';

const store = useGameStore();

const COL_LABELS = 'ABCDEFGHIJKLMNO';

function coordLabel(row: number, col: number): string {
  return `${COL_LABELS[col]}${BOARD_SIZE - row}`;
}

const BOARD_SIZE = 15;

let pollTimer: ReturnType<typeof setInterval> | null = null;

function refreshGames() {
  store.fetchOngoingGames();
}

onMounted(() => {
  refreshGames();
  pollTimer = setInterval(refreshGames, 5000);
});

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer);
  pollTimer = null;
});
</script>
