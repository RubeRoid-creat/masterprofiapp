import { Paper, Typography, Box } from '@mui/material';

export default function StatCard({ title, value, icon, color, subtitle }) {
  return (
    <Paper
      elevation={2}
      sx={{
        p: 3,
        display: 'flex',
        flexDirection: 'column',
        height: '100%',
      }}
    >
      <Box display="flex" alignItems="center" justifyContent="space-between" mb={1}>
        <Box sx={{ color, fontSize: 40 }}>{icon}</Box>
        <Typography variant="h4" component="div" fontWeight="bold">
          {value}
        </Typography>
      </Box>
      <Typography variant="h6" color="text.secondary" gutterBottom>
        {title}
      </Typography>
      {subtitle && (
        <Typography variant="body2" color="text.secondary">
          {subtitle}
        </Typography>
      )}
    </Paper>
  );
}

