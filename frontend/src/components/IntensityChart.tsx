import {
  CategoryScale,
  Chart as ChartJs,
  Filler,
  Legend,
  LineElement,
  LinearScale,
  PointElement,
  Tooltip,
} from 'chart.js'
import { Line } from 'react-chartjs-2'

ChartJs.register(CategoryScale, LinearScale, PointElement, LineElement, Tooltip, Legend, Filler)

interface IntensityChartProps {
  dataPoints: Array<{ label: string; value: number }>
}

export function IntensityChart({ dataPoints }: IntensityChartProps) {
  const data = {
    labels: dataPoints.map((point) => point.label),
    datasets: [
      {
        label: 'Average intensity (events/min)',
        data: dataPoints.map((point) => point.value),
        borderColor: 'rgb(28, 107, 160)',
        backgroundColor: 'rgba(28, 107, 160, 0.2)',
        borderWidth: 2,
        fill: true,
        tension: 0.32,
      },
    ],
  }

  return (
    <div className="chart-card">
      <h3>Live Intensity Stream</h3>
      <div className="chart-card__canvas">
        <Line
          data={data}
          options={{
            animation: false,
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
              legend: { display: false },
            },
            scales: {
              y: {
                beginAtZero: true,
              },
            },
          }}
        />
      </div>
    </div>
  )
}
