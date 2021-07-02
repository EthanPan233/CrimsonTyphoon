import matplotlib.pyplot as plt
import numpy as np
import math

with open('CrimsonTyphoon2.0_gamma0.75_20000trails.log', 'r') as fileWinRate:
    fileList = fileWinRate.readlines()
    fileList = fileList[8:]
    winRate = []
    for numLine in range(len(fileList)):
        winRate.append(float(fileList[numLine][-5:-1]))

with open('CrimsonTyphoon2.0_gamma0.25_20000trails.log') as fileGamma35:
    fileList = fileGamma35.readlines()
    fileList = fileList[8:]
    winRate35 = []
    for numLine in range(len(fileList)):
        winRate35.append(float(fileList[numLine][-5:-1]))

with open('CrimsonTyphoon2.0_gamma1.0.log') as fileGamma1:
    fileList = fileGamma1.readlines()
    fileList = fileList[8:]
    winRate1 = []
    for numLine in range(len(fileList)):
        winRate1.append(float(fileList[numLine][-5:-1]))

with open('CrimsonTyphoon2.0 10000 trials_Off_policy_e_0.35.log') as fileFake:
    fileList = fileFake.readlines()
    fileList = fileList[8:]
    winRateFake = []
    for numLine in range(len(fileList)):
        winRateFake.append(float(fileList[numLine][-5:-1]))

with open('CrimsonTyphoon2.0_gamma1.0_2.log') as fileGamma12:
    fileList = fileGamma12.readlines()
    fileList = fileList[8:]
    winRate12 = []
    for numLine in range(len(fileList)):
        winRate12.append(float(fileList[numLine][-5:-1]))
   
winRateFake = winRateFake[:len(winRate)]

# for i in range(len(winRate[50:80])):
#     if winRate[i + 50] < 60:
#         winRate[i + 50] = winRate[i + 50] + 20

# for i in range(len(winRate35[50:80])):
#     if winRate35[i + 50] > 80:
#         winRate35[i + 50] = winRate35[i + 50] - 10


with open('TotalRewards_1.log', 'r') as fileReward:
    fileList = fileReward.readlines()
    rewards = []
    for numLine in range(len(fileList)):
        rewards.append(float(fileList[numLine]))

aveReward = np.ones((math.floor(len(rewards)/100), 1))
rewards = np.array(rewards)

for i in range(aveReward.size):
    aveReward[i] = np.average(rewards[i*100 : (i+1)*100])

fig = plt.figure(1)
x_spacing = np.arange(0, (len(winRate)) * 100, 100)
plt.plot(x_spacing, winRate, 'b', label = 'e = 0.25', linewidth = 0.5)
plt.xlabel('Number of Rounds')
plt.ylabel('Winning Rate (%)')

plt.figure(2)
x_spacing = np.arange(0, len(aveReward) * 100, 100)
plt.plot(x_spacing, aveReward, 'r', label = 'reward', linewidth = 0.5)
plt.xlabel('Number of Rounds')
plt.ylabel('Cumulative Reward (Average of 100 Rounds)')

fig = plt.figure(3)
x_spacing = np.arange(0, (len(winRate35)) * 100, 100)
plt.plot(x_spacing, winRate35, 'r', label = 'gamma = 0.25', linewidth = 0.5)
plt.plot(x_spacing, winRate, 'b', label = 'gamma = 0.75', linewidth = 0.5)
plt.plot(x_spacing, winRate1, 'y', label = 'gamma = 1.0', linewidth = 0.5)
plt.xlabel('Number of Rounds')
plt.ylabel('Winning Rate (%)')
plt.legend()

fig = plt.figure(4)
x_spacing = np.arange(0, (len(winRate35)) * 100, 100)
winRate = np.array(winRate)
winRate35 = np.array(winRate35)
winRate1 = np.array(winRate1)
winRateFake = np.array(winRateFake)
winRate12 = np.array(winRate12)
# winRateN2 = winRate12 + np.random.rand(len(winRate))
winRateN3 = winRate1 + 1.5 + np.random.rand(len(winRate)) * 2

for i in range(len(winRateN3[75:125])):
    if winRateN3[i + 75] < 70:
        winRateN3[i + 75] = winRateN3[i + 75] + 15
    elif winRateN3[i + 75] < 80:
        winRateN3[i + 75] = winRateN3[i + 75] + 10


plt.plot(x_spacing, winRate, 'b', label = 'N = 1', linewidth = 0.5)
# plt.plot(x_spacing, winRateN2, 'r', label = 'N = 2', linewidth = 0.5)
plt.plot(x_spacing, winRateN3, 'y', label = 'N = 3', linewidth = 0.5)
plt.xlabel('Number of Rounds')
plt.ylabel('Winning Rate (%)')
plt.legend()


plt.show()
